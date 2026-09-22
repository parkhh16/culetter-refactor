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
        COMPLETED
    }
}