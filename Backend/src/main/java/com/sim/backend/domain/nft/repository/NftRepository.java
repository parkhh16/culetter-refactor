package com.sim.backend.domain.nft.repository;

import com.sim.backend.domain.letter.LetterEntity;
import com.sim.backend.domain.nft.entity.NftEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;

public interface NftRepository extends JpaRepository<NftEntity, Long>, NftQueryRepository {
    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
           set n.keyRef = :keyRef
         where n.letter = :letter
    """)
    void setKeyRefByLetter(LetterEntity letter, byte[] keyRef);

    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
        set n.letterCid = :letterCid, n.metadataCid = :metadataCid, n.audioCid = :audioCid
        where n.letter = :letter
    """)
    void setCidsByLetter(LetterEntity letter, String letterCid, String metadataCid, String audioCid);

    @Query("""
        select n.keyRef
          from NftEntity n
         where n.tokenId = :tokenId
    """)
    byte[] findRefKeyByTokenId(BigInteger tokenId);

    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
        set n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.READY_TO_MINT
        where n.reservationDate <= :now
        and n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.PENDING_DATE
    """)
    void modifyStatusDuePassedNfts(LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
        set n.tokenId = :tokenId, n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.MINTED_ONCHAIN
        where n.id = :id
    """)
    void markMintedOnchain(Long id, BigInteger tokenId);

    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
        set n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.MINT_FAILED
        where n.id = :id
    """)
    void markMintFailed(Long id);

    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
        set n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.READY_TO_MINT
        where n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.MINT_FAILED
    """)
    void resetFailedForRetry();

    @Modifying
    @Transactional
    @Query("""
        update NftEntity n
        set n.status = com.sim.backend.domain.nft.entity.NftEntity.NftStatus.COMPLETED
        where n.id = :id
    """)
    void tokenizationFinished(Long id);

    @Query("""
        select n.tokenId
          from NftEntity n
         where n.letter = :letter
    """)
    BigInteger findTokenIdByLetter(LetterEntity letter);
}
