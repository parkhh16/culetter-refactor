package com.sim.backend.domain.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryRepository extends JpaRepository<StoryEntity, Long> {

    // ERD의 ix_stories_user 인덱스 활용: user_id로 현재 진행 중인 스토리 조회
    Optional<StoryEntity> findByUserIdAndStatus(Long userId, StoryEntity.StoryStatus status);

    Optional<StoryEntity> findById(Long id);

    // ERD의 ix_stories_user_period 인덱스 활용: 사용자별 스토리 목록 조회
    List<StoryEntity> findByUserId(Long userId);
}