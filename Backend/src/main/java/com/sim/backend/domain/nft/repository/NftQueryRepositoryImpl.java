package com.sim.backend.domain.nft.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sim.backend.domain.letter.QLetterEntity;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.entity.QNftEntity;
import com.sim.backend.domain.users.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

    public Optional<NftEntity> pickOneNft() {
        QNftEntity n = QNftEntity.nftEntity;
        NftEntity row = queryFactory.selectFrom(n)
                .where(n.status.eq(NftEntity.NftStatus.READY_TO_MINT))
                .orderBy(n.reservationDate.asc(), n.id.asc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(row);
    }
}
