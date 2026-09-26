package com.sim.backend.domain.nft.controller;

import com.sim.backend.domain.nft.dto.request.MintingRequestDTO;
import com.sim.backend.domain.nft.dto.request.RenderNftRequestDTO;
import com.sim.backend.domain.nft.dto.response.RenderNftResponseDTO;
import com.sim.backend.domain.nft.dto.response.SentOrRecievedNftResponseDTO;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.domain.nft.service.NftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import com.sim.backend.domain.nft.dto.request.MinioEventRequestDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/nft")
@RequiredArgsConstructor
@Slf4j
public class NftController {

    private final NftRepository nftRepository;
    private final NftService nftService;

    /**
     * 사용자가 토큰화하고자 할 때 편지 번호, 수신자의 지갑주소, 받을 날짜를 DB에 저장한다.
     * (Minio에서 음성 학습을 지원하기 위함이고 시간 차가 존재하므로 비동기적으로 진행)
     * */
    @PostMapping("/minting")
    public ResponseEntity<String> mintingRequest(@RequestBody MintingRequestDTO mintingRequestDTO, @RequestHeader("Authorization") String authToken) {
        nftService.requestSaveToNftDatabase(mintingRequestDTO, authToken);
        return ResponseEntity.ok("NFT minting requested");
    }

    /**
     * 1. 완성된 음성파일을 Minio에서부터 백엔드로 불러온다.
     * 2. 음성파일과 기존 편지를 IPFS로 암호화 하여 업로드한다.
     * 3. 두 가지의 Cid를 담은 metadata를 만들어 한번 더 IPFS로 업로드 한다.
     * */
    @PostMapping("/webhooks/minio")
    public ResponseEntity<String> getAudioAndUploadPinata(@RequestBody MinioEventRequestDTO event) throws Exception {
        String key = event.getKey();

        // 초기화
        Long letterId = null;
        String[] parts = key.split("/");
        // 0:voice-logs, 1:firebaseUid, 2:letterId, 3:results, 4:filename
        if (parts.length >= 5 && "results".equals(parts[3])) {
            letterId = Long.valueOf(parts[2]);
        }

        // results 안의 .wav 파일만 처리
        String presignedUrl = nftService.presignResultsWav(key, 3600);
        if (presignedUrl == null) {
            return ResponseEntity.ok("Ignored: not a results/*.wav object");
        }

        nftService.uploadPinataAndMinting(letterId, presignedUrl);
        return ResponseEntity.ok("NFT 대기 중");
    }

    // DB로 부터 발신함 내용 조회
    @GetMapping("/sentTokens")
    public ResponseEntity<List<SentOrRecievedNftResponseDTO>> getSentTokens(@RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(nftService.getSentTokens(authToken));
    }

    // DB로 부터 수신함 내용 조회
    @GetMapping("/receivedTokens")
    public ResponseEntity<List<SentOrRecievedNftResponseDTO>> getReceivedTokens(@RequestHeader("Authorization") String authToken) {
        return ResponseEntity.ok(nftService.getReceivedTokens(authToken));
    }

    // Metamask / SmartContract로 부터 수신한 NFT 상세 조회
    @GetMapping("/rendering")
    public ResponseEntity<RenderNftResponseDTO> oneNftRendering(@RequestParam Long letterId, @RequestHeader("Authorization") String authToken) throws Exception {
        return ResponseEntity.ok(nftService.oneNftRendering(letterId, authToken));
    }

    /**
     * 백엔드에서 5초마다 현재 시각과 DB의 nft table에 있는 수신 날짜와 비교하며
     * 정해진 시간이 되었을 때 nft 토큰 화를 진행하여 수신자의 지갑주소로 송신한다..
     */
    @Scheduled(fixedDelay = 60_000)
    public void mintToSmartContract() {
        nftRepository.modifyStatusDuePassedNfts(LocalDateTime.now());
        // 이전 실행에서 실패해 MINT_FAILED로 남은 건을 이번 실행부터 다시 재시도 대상으로 되돌린다.
        // (실패 즉시 되돌리면 같은 실행의 while 루프 안에서 곧바로 재선택되어 무한루프에 빠진다)
        nftRepository.resetFailedForRetry();
        while (true) {
            Optional<NftEntity> claimed = nftRepository.pickAndClaimNft();
            if (claimed.isEmpty()) break;
            NftEntity nft = claimed.get();

            try {
                // MINTED_ONCHAIN으로 집힌 건은 이전 시도에서 온체인 mint까지는 성공한 것이므로 재민팅하지 않는다.
                if (nft.getStatus() != NftEntity.NftStatus.MINTED_ONCHAIN) {
                    nftService.mintToSmartContract(nft);
                }
                nftRepository.tokenizationFinished(nft.getId());
            } catch (Exception e) {
                // 어떤 예외든 이 건만 재시도 대상으로 되돌리고, 나머지 대기 중인 NFT는 계속 처리한다.
                // 전체 스택트레이스 대신 타입+메시지만 남긴다 (재시도 루프 특성상 반복 발생 가능, 로그 비용 최소화).
                log.error("NFT {} 민팅 실패, 다음 실행에서 재시도 예정: {}: {}",
                        nft.getId(), e.getClass().getSimpleName(), e.getMessage());
                nftRepository.markMintFailed(nft.getId());
            }
        }
    }
}
