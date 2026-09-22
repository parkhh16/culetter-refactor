package com.sim.backend.domain.retrospect;

import com.sim.backend.domain.retrospect.dto.RetrospectRequestDto;
import com.sim.backend.domain.retrospect.dto.RetrospectResponseDto;
import com.sim.backend.domain.retrospect.dto.RetrospectAllResponseDto;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
@Service
@Transactional
public class RetrospectService {

    @Autowired
    private RetrospectRepository retrospectRepository;

    @Autowired
    private StoryRepository storyRepository;

    /**
     * 회고 생성 - 오늘 날짜로 회고 생성
     */
    public RetrospectResponseDto createRetrospect(RetrospectRequestDto requestDto, UserEntity user) {
        // 활성 스토리 조회 (IN_PROGRESS 또는 PENDING_LETTER)
        StoryEntity story = findActiveStory(user);

        LocalDate today = LocalDate.now();

        // 이미 오늘 회고가 있는지 확인
        if (retrospectRepository.existsByStoryIdAndEntryDate(story.getId(), today)) {
            throw new RuntimeException("중복 회고");
        }

        // 회고 생성
        RetrospectEntity retrospect = new RetrospectEntity();
        retrospect.setStory(story);
        retrospect.setEntryDate(today);
        retrospect.setTitle(requestDto.getTitle());
        retrospect.setContent(requestDto.getContent());

        RetrospectEntity savedRetrospect = retrospectRepository.save(retrospect);
        return RetrospectResponseDto.from(savedRetrospect);
    }

    /**
     * 특정 날짜 회고 조회
     */
    @Transactional(readOnly = true)
    public RetrospectResponseDto getRetrospectByDate(LocalDate date, UserEntity user) {
        // 활성 스토리 조회
        StoryEntity story = findActiveStory(user);

        // 해당 날짜 회고 조회
        Optional<RetrospectEntity> retrospect = retrospectRepository.findByStoryIdAndEntryDate(story.getId(), date);

        if(retrospect.isPresent()) return RetrospectResponseDto.from(retrospect.get());
        else return null;
    }

    /**
     * 현재 활성 스토리의 전체 회고 조회
     */
    @Transactional(readOnly = true)
    public RetrospectAllResponseDto getAllRetrospects(UserEntity user) {
        // 활성 스토리 조회
        StoryEntity story = findActiveStory(user);

        // 해당 스토리의 모든 회고 조회
        List<RetrospectEntity> retrospectEntities = retrospectRepository.findByStoryIdOrderByEntryDateAsc(story.getId());

        // DTO 변환
        List<RetrospectAllResponseDto.RetrospectSimpleDto> retrospects = retrospectEntities.stream()
            .map(r -> new RetrospectAllResponseDto.RetrospectSimpleDto(r.getEntryDate(), r.getTitle(), r.getContent()))
            .toList();

        // 회고가 없으면 예외 발생
        if (retrospects.isEmpty()) {
            throw new RuntimeException("회고가 1건이상 존재하지 않습니다.");
        }

        // 응답 DTO 생성
        RetrospectAllResponseDto response = new RetrospectAllResponseDto();
        response.setStoryId(story.getId());
        response.setTheme(story.getTheme());
        response.setColor(story.getColor());
        response.setRetrospects(retrospects);
        response.setTotalCount(retrospects.size());

        return response;
    }

    /**
     * 활성 스토리 조회 (IN_PROGRESS 또는 PENDING_LETTER)
     */
    private StoryEntity findActiveStory(UserEntity user) {
        // 먼저 진행 중인 스토리 조회
        StoryEntity story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                .orElse(null);

        // 진행 중인 스토리가 없으면 편지화 대기 중인 스토리 조회
        if (story == null) {
            story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER)
                    .orElseThrow(() -> new RuntimeException("진행 중인 스토리를 찾을 수 없습니다."));
        }

        return story;
    }

    /**
     * 회고 삭제
     */
    public void deleteRetrospect(Long retrospectId, UserEntity user) {
        // 회고 조회
        RetrospectEntity retrospect = retrospectRepository.findById(retrospectId)
                .orElseThrow(() -> new RuntimeException("회고를 찾을 수 없습니다"));

        // 권한 확인 (해당 사용자의 스토리에 속한 회고인지 확인)
        if (!retrospect.getStory().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("권한이 없습니다");
        }

        // 회고 삭제
        retrospectRepository.delete(retrospect);
    }
}