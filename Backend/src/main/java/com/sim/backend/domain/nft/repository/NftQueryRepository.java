package com.sim.backend.domain.nft.repository;

import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.users.UserEntity;

import java.util.List;
import java.util.Optional;

public interface NftQueryRepository {
    List<NftEntity> findSentNfts(UserEntity user);
    List<NftEntity> findReceivedNfts(UserEntity user);

    // FOR UPDATE SKIP LOCKED로 pick과 상태 전환(IN_PROGRESS)을 한 트랜잭션에서 원자적으로 수행한다.
    Optional<NftEntity> pickAndClaimNft();
}
