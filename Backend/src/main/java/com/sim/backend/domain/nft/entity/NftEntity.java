package com.sim.backend.domain.nft.entity;

import com.sim.backend.domain.letter.LetterEntity;
import com.sim.backend.domain.users.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Table(name = "nfts")
@Getter
@Builder
@AllArgsConstructor
@Setter // TODO : 괜찮나?
public class NftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "nft_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(name = "receiver_email", nullable = false)
    private String receiverEmail;

    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    private LetterEntity letter;

    @Enumerated(EnumType.STRING)
    @Column
    private NftStatus status;

    @Column(name = "token_id")
    private BigInteger tokenId;

    @Column(name = "letter_cid")
    private String letterCid;

    @Column(name = "metadata_cid")
    private String metadataCid;

    @Column(name = "audio_cid")
    private String audioCid;

    @Column(name = "key_ref")
    private byte[] keyRef;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum NftStatus {
        PENDING_DATE,
        READY_TO_MINT,
        IN_PROGRESS,
        // 온체인 mint(Transfer 이벤트 확인)까지는 성공했지만 DB 완료 처리(COMPLETED)가 아직 안 된 상태.
        // 이 상태에서 재시도할 때는 재민팅하지 않고 완료 처리만 마저 진행한다.
        MINTED_ONCHAIN,
        COMPLETED,
        // 이번 스케줄러 실행에서 처리 실패한 건. 같은 실행 안에서 곧바로 재시도되어 무한루프에 빠지지
        // 않도록 pickAndClaimNft() 대상에서 제외하고, 다음 스케줄 실행 시작 시 READY_TO_MINT로 되돌린다.
        MINT_FAILED
    }
}