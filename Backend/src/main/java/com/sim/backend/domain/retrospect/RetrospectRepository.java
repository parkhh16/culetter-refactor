package com.sim.backend.domain.retrospect;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.sim.backend.domain.retrospect.dto.RetrospectAllResponseDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetrospectRepository extends JpaRepository<RetrospectEntity, Long>, RetrospectQueryRepository {

    /**
     * 특정 스토리의 특정 날짜 회고 조회
     */
    Optional<RetrospectEntity> findByStoryIdAndEntryDate(Long storyId, LocalDate entryDate);

    /**
     * 특정 스토리의 특정 날짜 회고 존재 여부 확인
     */
    boolean existsByStoryIdAndEntryDate(Long storyId, LocalDate entryDate);

    Optional<RetrospectEntity> findByStoryIdAndEntryDateBetweenOrderByCreatedAtAsc(Long storyId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM RetrospectEntity r WHERE r.story.id = :storyId ORDER BY r.entryDate ASC")
    List<RetrospectEntity> findByStoryIdOrderByEntryDateAsc(@Param("storyId") Long storyId);
}