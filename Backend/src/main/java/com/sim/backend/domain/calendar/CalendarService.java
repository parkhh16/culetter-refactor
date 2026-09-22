package com.sim.backend.domain.calendar;

import com.sim.backend.domain.calendar.dto.CalendarDetailResponseDto;
import com.sim.backend.domain.record.RecordEntity;
import com.sim.backend.domain.record.RecordRepository;
import com.sim.backend.domain.retrospect.RetrospectEntity;
import com.sim.backend.domain.retrospect.RetrospectRepository;
import com.sim.backend.domain.retrospect.dto.RetrospectResponseDto;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final RetrospectRepository retrospectRepository;
    private final RecordRepository recordRepository;

    @Transactional(readOnly = true)
    public List<RetrospectResponseDto> getCalendarList(String uid, int year, int month){
        Optional<UserEntity> userOptional = userRepository.findByFirebaseUid(uid);

        if(userOptional.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        UserEntity user = userOptional.get();

        // 해당 사용자의 모든 스토리 조회
        List<StoryEntity> userStories = storyRepository.findByUserId(user.getId());

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 모든 스토리의 회고를 조회
        return userStories.stream()
                .flatMap(story -> retrospectRepository.findByStoryIdOrderByEntryDateAsc(story.getId()).stream())
                .filter(entity -> !entity.getEntryDate().isBefore(startDate) && !entity.getEntryDate().isAfter(endDate))
                .map(RetrospectResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoryEntity getActiveStory(String uid) {
        Optional<UserEntity> userOptional = userRepository.findByFirebaseUid(uid);

        if(userOptional.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }

        UserEntity user = userOptional.get();
        return storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                .or(() -> storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER))
                .orElseThrow(() -> new RuntimeException("활성 스토리가 없습니다."));
    }

    public int calculateProgress(StoryEntity story, List<RetrospectResponseDto> retrospects) {
        LocalDate startDate = story.getStartedAt();
        LocalDate endDate = story.getEndedAt();

        if (startDate == null || endDate == null) {
            return 0;
        }

        LocalDate currentDate = LocalDate.now();

        // 아직 시작 전이면 0%
        if (currentDate.isBefore(startDate)) {
            return 0;
        }

        // 이미 종료됐으면 100%
        if (currentDate.isAfter(endDate)) {
            return 100;
        }

        long totalDays = startDate.datesUntil(endDate.plusDays(1)).count();
        long elapsedDays = startDate.datesUntil(currentDate.plusDays(1)).count();

        return (int) Math.round((double) elapsedDays / totalDays * 100);
    }

    @Transactional(readOnly = true)
    public CalendarDetailResponseDto getCalendarDetail(String uid, LocalDate date, RetrospectResponseDto retrospect){
        CalendarDetailResponseDto dto = new CalendarDetailResponseDto();
        Optional<UserEntity> userOptional = userRepository.findByFirebaseUid(uid);

        UserEntity user;
        StoryEntity story = null;

        if(userOptional.isPresent()){
            user = userOptional.get();
            story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                    .or(() -> storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER))
                    .orElse(null);
        }

        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(LocalTime.MAX);

        List<RecordEntity> records = recordRepository.findByStoryIdAndCreatedAtBetweenOrderByCreatedAtAscWithStory(
                story.getId(), startDate, endDate
        );

        List<CalendarDetailResponseDto.RecordDto> convertedRecords = records
                .stream()
                .map(CalendarDetailResponseDto.RecordDto::from)
                .toList();

        dto.setRetrospect(retrospect);
        dto.setRecords(convertedRecords);
        return dto;
    }
}
