package com.sim.backend.domain.nft.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sim.backend.domain.letter.QLetterEntity;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.entity.QNftEntity;
import com.sim.backend.domain.users.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NftQueryRepositoryImpl implements NftQueryRepository {

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager em;

    public List<NftEntity> findSentNfts(UserEntity user) {
        QNftEntity nft = QNftEntity.nftEntity;
        QLetterEntity letter = QLetterEntity.letterEntity;

        return queryFactory
                .selectFrom(nft)
                .join(nft.letter, letter).fetchJoin()
                .where(
                        nft.status.eq(NftEntity.NftStatus.COMPLETED),
                        nft.sender.eq(user)
                )
                .orderBy(nft.reservationDate.desc())
                .fetch();
    }

    public List<NftEntity> findReceivedNfts(UserEntity user) {
        QNftEntity nft = QNftEntity.nftEntity;
        QLetterEntity letter = QLetterEntity.letterEntity;

        return queryFactory
                .selectFrom(nft)
                .join(nft.letter, letter).fetchJoin()
                .where(
                        nft.status.eq(NftEntity.NftStatus.COMPLETED),
                        nft.receiverEmail.eq(user.getEmail())
                )
                .orderBy(nft.reservationDate.desc())
                .fetch();
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Optional<NftEntity> pickAndClaimNft() {
        // Hibernate의 lock.timeout=-2 힌트는 방언에 따라 SKIP LOCKED로 번역되지 않을 수 있어
        // SQL에 직접 명시하는 native query로 처리한다 (H2 2.3 / MySQL 8.0+ 모두 지원).
        List<NftEntity> rows = em.createNativeQuery("""
                SELECT * FROM nfts
                 WHERE status IN ('READY_TO_MINT', 'MINTED_ONCHAIN')
                 ORDER BY reservation_date ASC, nft_id ASC
                 LIMIT 1
                 FOR UPDATE SKIP LOCKED
                """, NftEntity.class).getResultList();

        if (rows.isEmpty()) {
            return Optional.empty();
        }
        NftEntity row = rows.get(0);
        if (row.getStatus() == NftEntity.NftStatus.READY_TO_MINT) {
            row.setStatus(NftEntity.NftStatus.IN_PROGRESS);
        }
        return Optional.of(row);
    }
}
