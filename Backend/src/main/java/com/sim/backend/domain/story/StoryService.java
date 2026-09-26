package com.sim.backend.domain.story;

import com.sim.backend.domain.story.dto.MainTabResponseDto;
import com.sim.backend.domain.story.dto.StoryRequestDto;
import com.sim.backend.domain.story.dto.StoryResponseDto;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.domain.record.RecordEntity;
import com.sim.backend.domain.record.RecordRepository;
import com.sim.backend.domain.retrospect.RetrospectEntity;
import com.sim.backend.domain.retrospect.RetrospectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class StoryService {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RetrospectRepository retrospectRepository;


    public StoryResponseDto createStory(StoryRequestDto requestDto, UserEntity user) {

        // 진행 중이거나 편지화 대기 중인 스토리가 있는지 확인
        if (storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS).isPresent() ||
            storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER).isPresent()) {
            throw new RuntimeException("이미 진행 중이거나 편지화 대기 중인 스토리가 있습니다. 기존 스토리를 완료한 후 새 스토리를 만들 수 있습니다.");
        }

        LocalDate today = LocalDate.now();

        StoryEntity story = new StoryEntity();
        story.setUser(user);
        story.setTheme(requestDto.getTheme());
        story.setColor(requestDto.getColor());
        story.setStartedAt(today);  // 오늘 날짜로 자동 설정
        story.setEndedAt(requestDto.getEndedAt());

        // 상태는 항상 IN_PROGRESS (오늘 시작하므로)
        story.setStatus(StoryEntity.StoryStatus.IN_PROGRESS);

        StoryEntity savedStory = storyRepository.save(story);
        return StoryResponseDto.from(savedStory);
    }

    public MainTabResponseDto getMainTab(Long storyId, LocalDate date) {
        StoryEntity story = storyRepository.findById(storyId)
                .orElseThrow(() -> new RuntimeException("스토리를 찾을 수 없습니다"));

        return buildMainTabResponse(story, date);
    }

    @Transactional
    public MainTabResponseDto getCurrentUserMainTab(LocalDate date, UserEntity user) {

        // 먼저 진행 중인 스토리 조회
        StoryEntity story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                .orElse(null);

        // 진행 중인 스토리가 없으면 편지화 대기 중인 스토리 조회
        if (story == null) {
            story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER)
                    .orElseThrow(() -> new RuntimeException("활성 스토리가 없습니다"));
        }

        // 기간 종료됐는데 IN_PROGRESS면 자동으로 PENDING_LETTER로 변경
        LocalDate today = LocalDate.now();
        if (story.getStatus() == StoryEntity.StoryStatus.IN_PROGRESS && today.isAfter(story.getEndedAt())) {
            story.setStatus(StoryEntity.StoryStatus.PENDING_LETTER);
            storyRepository.save(story);
        }

        return buildMainTabResponse(story, date);
    }

    // 공통 로직 분리
    private MainTabResponseDto buildMainTabResponse(StoryEntity story, LocalDate date) {
        MainTabResponseDto response = new MainTabResponseDto();
        response.setStoryId(story.getId());
        response.setTheme(story.getTheme());
        response.setColor(story.getColor());
        response.setStatus(story.getStatus());
        response.setDate(date);

        // 날짜 계산
        int daysFromStart = (int) ChronoUnit.DAYS.between(story.getStartedAt(), date);
        int daysToEnd = (int) ChronoUnit.DAYS.between(date, story.getEndedAt());
        response.setDaysFromStart(Math.max(0, daysFromStart));
        response.setDaysToEnd(Math.max(0, daysToEnd));

        // 실제 회고 데이터 조회
        RetrospectEntity retrospectEntity = retrospectRepository.findByStoryIdAndEntryDate(story.getId(), date)
                .orElse(null);

        if (retrospectEntity != null) {
            MainTabResponseDto.RetrospectDto retrospect = new MainTabResponseDto.RetrospectDto();
            retrospect.setId(retrospectEntity.getId());
            retrospect.setTitle(retrospectEntity.getTitle());
            retrospect.setContent(retrospectEntity.getContent());
            response.setRetrospect(retrospect);
        } else {
            response.setRetrospect(null);  // 회고가 없으면 null
        }

        // 실제 레코드 데이터 조회 (해당 날짜만)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<RecordEntity> recordEntities = recordRepository.findByStoryIdAndCreatedAtBetweenOrderByCreatedAtAscWithStory(
                story.getId(), startOfDay, endOfDay);
        List<MainTabResponseDto.RecordDto> records = new ArrayList<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (RecordEntity recordEntity : recordEntities) {
            MainTabResponseDto.RecordDto record = new MainTabResponseDto.RecordDto();
            record.setId(recordEntity.getId());
            record.setTime(recordEntity.getCreatedAt().format(timeFormatter));
            record.setTranscriptText(recordEntity.getTranscriptText());
            record.setSummaryText(recordEntity.getSummaryText() != null ? recordEntity.getSummaryText() : "요약 없음");
            records.add(record);
        }

        response.setRecords(records);
        response.setRecordsCount(records.size());

        return response;
    }
}