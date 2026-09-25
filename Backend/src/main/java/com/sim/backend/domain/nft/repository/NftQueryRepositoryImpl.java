package com.sim.backend.domain.nft.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sim.backend.domain.letter.QLetterEntity;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.entity.QNftEntity;
import com.sim.backend.domain.users.UserEntity;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NftQueryRepositoryImpl implements NftQueryRepository {

    private final JPAQueryFactory queryFactory;

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
    public Optional<NftEntity> pickAndClaimNft() {
        QNftEntity n = QNftEntity.nftEntity;
        NftEntity row = queryFactory.selectFrom(n)
                .where(n.status.in(NftEntity.NftStatus.READY_TO_MINT, NftEntity.NftStatus.MINTED_ONCHAIN))
                .orderBy(n.reservationDate.asc(), n.id.asc())
                .limit(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", -2) // Hibernate: -2 = SKIP LOCKED
                .fetchOne();

        if (row == null) {
            return Optional.empty();
        }
        if (row.getStatus() == NftEntity.NftStatus.READY_TO_MINT) {
            row.setStatus(NftEntity.NftStatus.IN_PROGRESS);
        }
        return Optional.of(row);
    }
}
