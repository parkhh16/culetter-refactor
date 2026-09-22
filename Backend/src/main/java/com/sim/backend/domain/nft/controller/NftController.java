package com.sim.backend.domain.nft.controller;

import com.sim.backend.domain.nft.dto.request.MintingRequestDTO;
import com.sim.backend.domain.nft.dto.request.RenderNftRequestDTO;
import com.sim.backend.domain.nft.dto.response.RenderNftResponseDTO;
import com.sim.backend.domain.nft.dto.response.SentOrRecievedNftResponseDTO;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.domain.nft.service.NftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.sim.backend.domain.nft.dto.request.MinioEventRequestDTO;
import org.web3j.protocol.exceptions.TransactionException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/nft")
@RequiredArgsConstructor
public class NftController {

    private final NftRepository nftRepository;
    private final NftService nftService;

    /**
     * 사용자가 토큰화하고자 할 때 편지 번호, 수신자의 지갑주소, 받을 날짜를 DB에 저장한다.
     * (Minio에서 음성 학습을 지원하기 위함이고 시간 차가 존재하므로 비동기적으로 진행)
     * */
    @PostMapping("/minting")
    public ResponseEntity<String> mintingRequest(@RequestBody MintingRequestDTO mintingRequestDTO, @RequestHeader("Authorization") String authToken) {
        try {
            nftService.requestSaveToNftDatabase(mintingRequestDTO, authToken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok("NFT minting requested");
    }

    /**
     * 1. 완성된 음성파일을 Minio에서부터 백엔드로 불러온다.
     * 2. 음성파일과 기존 편지를 IPFS로 암호화 하여 업로드한다.
     * 3. 두 가지의 Cid를 담은 metadata를 만들어 한번 더 IPFS로 업로드 한다.
     * */
    @PostMapping("/webhooks/minio")
    public ResponseEntity<String> getAudioAndUploadPinata(@RequestBody MinioEventRequestDTO event) {
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

        try {
            nftService.uploadPinataAndMinting(letterId, presignedUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return ResponseEntity.ok("NFT 대기 중");
    }

    // DB로 부터 발신함 내용 조회
    @GetMapping("/sentTokens")
    public ResponseEntity<List<SentOrRecievedNftResponseDTO>> getSentTokens(@RequestHeader("Authorization") String authToken) {
        try {
            return ResponseEntity.ok(nftService.getSentTokens(authToken));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // DB로 부터 수신함 내용 조회
    @GetMapping("/receivedTokens")
    public ResponseEntity<List<SentOrRecievedNftResponseDTO>> getReceivedTokens(@RequestHeader("Authorization") String authToken) {
        try {
            return ResponseEntity.ok(nftService.getReceivedTokens(authToken));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Metamask / SmartContract로 부터 수신한 NFT 상세 조회
    @GetMapping("/rendering")
    public ResponseEntity<RenderNftResponseDTO> oneNftRendering(@RequestParam Long letterId, @RequestHeader("Authorization") String authToken) {
        try {
            return ResponseEntity.ok(nftService.oneNftRendering(letterId, authToken));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 백엔드에서 5초마다 현재 시각과 DB의 nft table에 있는 수신 날짜와 비교하며
     * 정해진 시간이 되었을 때 nft 토큰 화를 진행하여 수신자의 지갑주소로 송신한다..
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void mintToSmartContract() {
        nftRepository.modifyStatusDuePassedNfts(LocalDateTime.now());
        while (true) {
            Optional<NftEntity> ready = nftRepository.pickOneNft();
            if (ready.isEmpty()) break;
            NftEntity nft = ready.get();

            nftRepository.tokenizationStarted(nft.getId());

            try {
                nftService.mintToSmartContract(nft);
                nftRepository.tokenizationFinished(nft.getId());
            } catch (IOException | TransactionException e) {
                throw new IllegalStateException("receipt wait/parse failed: " + e.getMessage(), e);
            }
        }
    }
}
