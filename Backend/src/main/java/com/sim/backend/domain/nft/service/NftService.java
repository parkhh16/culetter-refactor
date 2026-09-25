package com.sim.backend.domain.nft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sim.backend.domain.letter.LetterEntity;
import com.sim.backend.domain.letter.LetterRepository;
import com.sim.backend.domain.letter.LetterService;
import com.sim.backend.domain.nft.dto.request.MintingRequestDTO;
import com.sim.backend.domain.nft.dto.response.SentOrRecievedNftResponseDTO;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.global.util.CryptoUtil;
import com.sim.backend.global.util.FirebaseTokenUtil;
import com.sim.backend.global.util.KeyUtil;
import com.sim.backend.global.util.MimeSniffer;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.domain.nft.dto.request.RenderNftRequestDTO;
import com.sim.backend.domain.nft.dto.response.RenderNftResponseDTO;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.utils.Numeric;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NftService {

    @Value("${contract.nft}")
    private String contractAddress;

    @Value("${minio.bucket}")
    private String bucket;

    private final FirebaseTokenUtil firebaseTokenUtil;
    private final UserRepository userRepository;

    private final MinioClient minioClient;
    private final Web3j web3j;
    private final TransactionManager txManager;

    private final WebClient.Builder webClientBuilder;
    private final PinataService pinataService;
    private final LetterService letterService;

    private final NftRepository nftRepository;
    private final LetterRepository letterRepository;

    // metaBytes -> json
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Event TRANSFER_EVENT = new Event("Transfer",
            java.util.Arrays.asList(
                    new TypeReference<Address>(true) {},  // from (indexed)
                    new TypeReference<Address>(true) {},  // to   (indexed)
                    new TypeReference<Uint256>(true) {}   // tokenId (indexed)
            )
    );
    private static final String TRANSFER_SIG = EventEncoder.encode(TRANSFER_EVENT);

    public void requestSaveToNftDatabase(MintingRequestDTO mintingRequestDTO, String authToken) {
        String firebaseUid = firebaseTokenUtil.extractFirebaseUid(authToken);
        UserEntity user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        Long letterId = mintingRequestDTO.getLetterId();

        // letter가 있는지 및 user의 letter인지 체크 용 -> response가 달라서 나눠 둠
        letterService.getLetterById(letterId, user);
        // 어차피 없는 경우는 없을 예정
        LetterEntity letter = letterRepository.findById(letterId).orElseThrow();

        // Status는 계속 변경 예정
        NftEntity nftEntity = NftEntity.builder()
                .sender(user)
                .receiverEmail(mintingRequestDTO.getReceiverEmail())
                .reservationDate(mintingRequestDTO.getReservationDate())
                .letter(letter)
                .status(NftEntity.NftStatus.PENDING_DATE)
                .build();

        nftRepository.save(nftEntity);
    }

    public void uploadPinataAndMinting(Long letterId, String presignedUrl) throws Exception {
        // 전역 빌더를 사용하되, 완전 초기화 + 스트리핑 필터 적용
        ExchangeFilterFunction stripAuthHeaders = (req, next) -> {
            ClientRequest filtered = ClientRequest.from(req)
                    .headers(h -> {
                        h.remove(HttpHeaders.AUTHORIZATION);
                        h.remove("x-amz-content-sha256");
                        h.remove("X-Amz-Content-Sha256");
                        h.remove(HttpHeaders.COOKIE);
                    })
                    .build();
            return next.exchange(filtered);
        };

        WebClient presignClient = webClientBuilder.clone()
                .filters(fs -> { fs.clear(); fs.add(stripAuthHeaders); })
                .defaultHeaders(h -> h.clear())
                .defaultCookies(c -> c.clear())
                .build();

        // 재조립/재인코딩 금지: 절대 URI 그대로 사용
        URI uri = URI.create(presignedUrl);

        byte[] audioBytes = presignClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                // 요청 단계에서도 혹시 모를 헤더를 한 번 더 제거
                .headers(h -> {
                    h.remove(HttpHeaders.AUTHORIZATION);
                    h.remove("x-amz-content-sha256");
                    h.remove("X-Amz-Content-Sha256");
                    h.remove(HttpHeaders.COOKIE);
                })
                .retrieve()
                .onStatus(s -> s.isError(), resp ->
                        resp.bodyToMono(String.class).defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        "MinIO GET failed: " + resp.statusCode() + " body=" + body))))
                .bodyToMono(byte[].class)
                .block();

        byte[] aesKey = KeyUtil.newAes256KeyBytes();

        LetterEntity letter = letterRepository.findById(letterId).orElseThrow();

        nftRepository.setKeyRefByLetter(letter, aesKey);

        ObjectNode objectNode = new ObjectMapper().createObjectNode()
                .put("title", letter.getTitle())
                .put("content", letter.getContent());

        pinataService.encryptedUploadAndMakeMetadata(letter, "NFT 제목", "NFT 설명", aesKey, audioBytes, new ObjectMapper().writeValueAsBytes(objectNode));
    }

    public List<SentOrRecievedNftResponseDTO> getSentTokens(String authToken) {
        String firebaseUid = firebaseTokenUtil.extractFirebaseUid(authToken);
        UserEntity user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<NftEntity> list = nftRepository.findSentNfts(user);
        return list.stream()
                .map(n -> new SentOrRecievedNftResponseDTO(
                        n.getLetter().getId(),
                        n.getReservationDate(),
                        n.getReceiverEmail(),
                        n.getLetter().getTitle()
                ))
                .toList();
    }

    public List<SentOrRecievedNftResponseDTO> getReceivedTokens(String authToken) {
        String firebaseUid = firebaseTokenUtil.extractFirebaseUid(authToken);
        UserEntity user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        List<NftEntity> list = nftRepository.findReceivedNfts(user);
        return list.stream()
                .map(n -> new SentOrRecievedNftResponseDTO(
                        n.getLetter().getId(),
                        n.getReservationDate(),
                        n.getReceiverEmail(),
                        n.getLetter().getTitle()
                ))
                .toList();
    }

    public RenderNftResponseDTO oneNftRendering(Long letterId, String authToken) throws Exception{
        String firebaseUid = firebaseTokenUtil.extractFirebaseUid(authToken);
        UserEntity user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        LetterEntity letter = letterRepository.findById(letterId).orElseThrow();

        String walletAddress = user.getWalletAddress();
        BigInteger tokenId = nftRepository.findTokenIdByLetter(letter);

        String ownersWalletAddress = ownerOf(tokenId);
        if (!ownersWalletAddress.equalsIgnoreCase(walletAddress)) {
            throw new IllegalStateException("not your nft");
        }

        // "ipfs/<cid>" 반환
        String tokenURI = tokenURI(tokenId);

        // 앞 ipfs를 잘라내는 함수
        String metadataCid = parseIpfsCid(tokenURI);

        // metadataCid 값으로 pinata에서 byte 형태로 metadata 호출
        byte[] metaBytes = pinataService.getBytes(metadataCid);

        // 호출한 byte형태 metadata를 json형태로 변환
        JsonNode metadata = objectMapper.readTree(metaBytes);

        // metadata 에서 암호화된 cid, mime, iv값 전부 다 가져오기 (복호화 하기 위해 저장)
        // Json 형태 참고
//        String imageCid = parseIpfsCid(metadata.path("image").asText(null));
        String audioCid = parseIpfsCid(metadata.path("audio").asText(null));
        String textCid  = parseIpfsCid(metadata.path("text").asText(null));

//        String imageMime = metadata.path("properties").path("image").path("mime_type").asText("image/png");
        String audioMime = metadata.path("properties").path("audio").path("mime_type").asText("audio/mpeg");
        String textMime  = metadata.path("properties").path("text").path("mime_type").asText("text/plain");

//        byte[] ivImage = b64Decoding(metadata.path("properties").path("image").path("iv_b64").asText(null));
        byte[] ivAudio = b64Decoding(metadata.path("properties").path("audio").path("iv_b64").asText(null));
        byte[] ivText  = b64Decoding(metadata.path("properties").path("text").path("iv_b64").asText(null));

        byte[] key = nftRepository.findRefKeyByTokenId(tokenId);

        // 복호화 과정
        String title = null, content =null, audioData = null; // imageData = null

//        if (imageCid != null && ivImage != null) {
//            byte[] enc = pinataService.getBytes(imageCid);
//            byte[] dec = CryptoUtil.decryptAesGcm(enc, key, ivImage);
//            imageMime = MimeSniffer.guessOrDefault(dec, imageMime);
//            imageData = "data:" + imageMime + ";base64," + Base64.getEncoder().encodeToString(dec);
//        }
        if (audioCid != null && ivAudio != null) {
            byte[] enc = pinataService.getBytes(audioCid);
            byte[] dec = CryptoUtil.decryptAesGcm(enc, key, ivAudio);
            audioMime = MimeSniffer.guessOrDefault(dec, audioMime);
            audioData = "data:" + audioMime + ";base64," + Base64.getEncoder().encodeToString(dec);
        }
        if (textCid != null && ivText != null) {
            byte[] enc = pinataService.getBytes(textCid);
            byte[] dec = CryptoUtil.decryptAesGcm(enc, key, ivText);

            String innerTextData = new String(dec, StandardCharsets.UTF_8);
            // 형식 맞추려면 밑의 코드로

            // textMime = MimeSniffer.guessOrDefault(dec, textMime);
            // textData = "data:" + textMime + ";base64," + Base64.getEncoder().encodeToString(dec);

            String b64 = objectMapper.readValue(innerTextData, String.class);
            byte[] inner = java.util.Base64.getDecoder().decode(b64);
            JsonNode textJson = objectMapper.readTree(inner);

            title = textJson.path("title").asText();
            content = textJson.path("content").asText();
        }

        return RenderNftResponseDTO.builder()
//                .tokenId(tokenId.longValue())
//                .imageData(imageData)
                .title(title)
                .content(content)
                .audioData(audioData)
                .build();
    }

    public void mintToSmartContract(NftEntity nftEntity) throws IOException, TransactionException {
        UserEntity receiver = userRepository.findByEmail(nftEntity.getReceiverEmail())
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        Function fn = new Function(
                "mint",
                List.of(new Address(receiver.getWalletAddress()), new Utf8String("ipfs://" + nftEntity.getMetadataCid())),
                List.of()
        );
        String data = FunctionEncoder.encode(fn);

        // 4) 가스 세팅: 프로젝트 넷은 0 가스라고 했으니 고정
        BigInteger gasPrice = BigInteger.ZERO;
        BigInteger gasLimit = BigInteger.valueOf(800_000); // 필요시 600k~1M 조절

        EthSendTransaction tx;
        try {
            tx = txManager.sendTransaction(gasPrice, gasLimit, contractAddress, data, BigInteger.ZERO);
        } catch (Exception e) {
            throw new IllegalStateException("mint send failed: " + e.getMessage(), e);
        }

        TransactionReceipt receipt = new PollingTransactionReceiptProcessor(web3j, 1500, 60)
                .waitForTransactionReceipt(tx.getTransactionHash());

        String ZERO = "0x0000000000000000000000000000000000000000";
        for (var log : receipt.getLogs()) {
            if (!contractAddress.equalsIgnoreCase(log.getAddress())) continue;
            var topics = log.getTopics();
            if (topics == null || topics.size() < 4) continue;
            if (!TRANSFER_SIG.equalsIgnoreCase(topics.get(0))) continue;

            String from = "0x" + topics.get(1).substring(26); // indexed address 디코드
            if (!ZERO.equalsIgnoreCase(from)) continue;        // 민팅만 필터
            // topics[3] = tokenId
            nftRepository.markMintedOnchain(nftEntity.getId(), Numeric.toBigInt(topics.get(3)));

            return;
        }
        throw new IllegalStateException("Transfer(from=0x0) event not found in receipt");
    }



    /** 스마트 컨트랙트 호출 함수 */

    // 아래의 메서드들이 작동되도록 트랜잭션 날려주는 메서드
    private List<Type> ethCall(Function fn) throws Exception {
        String data = FunctionEncoder.encode(fn);
        String from = getFromForCall();

        var resp = web3j.ethCall(
                Transaction.createEthCallTransaction(from, contractAddress, data),
                DefaultBlockParameterName.LATEST
        ).send();

        if (resp.isReverted()) throw new IllegalStateException("eth_call reverted: " + resp.getRevertReason());
        return FunctionReturnDecoder.decode(resp.getValue(), fn.getOutputParameters());
    }

    // NFT 번호 -> 주인이 누구인지 찾는 함수
    private String ownerOf(BigInteger tokenId) throws Exception {
        Function fn = new Function(
                "ownerOf",
                List.of(new Uint256(tokenId)),
                List.of(new TypeReference<Address>() {})
        );
        return ((Address) ethCall(fn).getFirst()).getValue();
    }

    // NFT 번호 -> metadata(nft) Cid값을 받아옴
    private String tokenURI(BigInteger tokenId) throws Exception {
        Function fn = new Function(
                "tokenURI",
                List.of(new Uint256(tokenId)),
                List.of(new TypeReference<Utf8String>() {})
        );
        return ((Utf8String) ethCall(fn).getFirst()).getValue();
    }

    private BigInteger balanceOf(String owner) throws Exception {
        Function fn = new Function(
                "balanceOf",
                List.of(new Address(owner)),
                List.of(new TypeReference<Uint256>() {})
        );
        return ((Uint256) ethCall(fn).getFirst()).getValue();
    }

    private BigInteger tokenOfOwnerByIndex(String owner, BigInteger index) throws Exception {
        Function fn = new Function(
                "tokenOfOwnerByIndex",
                List.of(new Address(owner), new Uint256(index)),
                List.of(new TypeReference<Uint256>() {})
        );
        return ((Uint256) ethCall(fn).getFirst()).getValue();
    }



    /** 부가 로직 */
    // 지갑 주소 검증 로직
    private static String requireAddress(String addr, String field) {
        if (addr == null || !addr.matches("^0x[0-9a-fA-F]{40}$"))
            throw new IllegalArgumentException(field + " must be 0x-prefixed 40-hex");
        return addr;
    }

    // from 주소 가져오기
    private String getFromForCall() {
        if (txManager instanceof RawTransactionManager rawTransactionManager) {
            return rawTransactionManager.getFromAddress();
        }
        return "0x0000000000000000000000000000000000000000";
    }

    // 받아온 url에서 (ipfs://를 잘라) cid만 뽑아내는 메서드
    private static String parseIpfsCid(String maybe) {
        if (maybe == null || maybe.isBlank()) return null;
        if (maybe.startsWith("ipfs://")) return maybe.substring("ipfs://".length());
        return maybe;
    }

    private static byte[] b64Decoding(String s) {
        return (s == null || s.isBlank()) ? null : Base64.getDecoder().decode(s);
    }

    /** Minio에서 부터 받아오기 */

    public String presignResultsWav(String objectKey, int expirySeconds) {
        if (objectKey == null || !objectKey.contains("results/") || !objectKey.endsWith(".wav")) {
            return null; // 대상이 아니면 null 반환
        }

        // key에 bucket이 붙어서 반환되어 bucket명을 지우는 용도
        if (objectKey.startsWith(bucket + "/")) {
            objectKey = objectKey.substring((bucket + "/").length());
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to presign URL: " + objectKey, e);
        }
    }
}
