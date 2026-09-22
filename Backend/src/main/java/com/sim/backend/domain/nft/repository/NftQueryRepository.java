package com.sim.backend.domain.nft.repository;

import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.users.UserEntity;

import java.util.List;
import java.util.Optional;

public interface NftQueryRepository {
    List<NftEntity> findSentNfts(UserEntity user);
    List<NftEntity> findReceivedNfts(UserEntity user);
    Optional<NftEntity> pickOneNft();
}
