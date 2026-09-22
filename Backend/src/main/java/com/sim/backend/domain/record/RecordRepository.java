package com.sim.backend.domain.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecordRepository extends JpaRepository<RecordEntity, Long> {

    // 특정 스토리의 레코드 목록 조회 (최신순)
    List<RecordEntity> findByStoryIdOrderByCreatedAtDesc(Long storyId);

    // 특정 스토리의 레코드 개수 조회
    int countByStoryId(Long storyId);

    // 특정 날짜의 레코드 목록 조회 (시간순)
    List<RecordEntity> findByStoryIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long storyId, LocalDateTime startTime, LocalDateTime endTime
    );

    // N+1 방지를 위한 Fetch Join 버전
    @Query("SELECT r FROM RecordEntity r JOIN FETCH r.story WHERE r.story.id = :storyId AND r.createdAt BETWEEN :startTime AND :endTime ORDER BY r.createdAt ASC")
    List<RecordEntity> findByStoryIdAndCreatedAtBetweenOrderByCreatedAtAscWithStory(
            @Param("storyId") Long storyId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // StoryService용 Fetch Join
    @Query("SELECT r FROM RecordEntity r JOIN FETCH r.story WHERE r.story.id = :storyId ORDER BY r.createdAt DESC")
    List<RecordEntity> findByStoryIdOrderByCreatedAtDescWithStory(@Param("storyId") Long storyId);
}