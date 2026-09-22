package com.sim.backend.domain.record;

import com.sim.backend.domain.record.dto.RecordRequestDto;
import com.sim.backend.domain.record.dto.RecordResponseDto;
import com.sim.backend.domain.record.dto.RecordListResponseDto;
import com.sim.backend.domain.record.dto.RecordDetailResponseDto;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    public RecordResponseDto createRecord(RecordRequestDto requestDto, UserEntity user) {

        // 현재 진행중인 스토리 또는 편지화 대기 중인 스토리 찾기
        StoryEntity story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                .orElseGet(() -> storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER)
                        .orElseThrow(() -> new RuntimeException("활성 스토리가 없습니다")));

        // PENDING_LETTER 상태면 레코드 생성 금지
        if (story.getStatus() == StoryEntity.StoryStatus.PENDING_LETTER) {
            throw new RuntimeException("스토리 기간이 종료되어 더 이상 기록을 추가할 수 없습니다. 편지화를 완료해주세요.");
        }

        // 레코드 생성
        RecordEntity record = new RecordEntity();
        record.setStory(story);
        record.setTranscriptText(requestDto.getTranscriptText());
        record.setSummaryText(requestDto.getSummaryText());

        RecordEntity savedRecord = recordRepository.save(record);
        return RecordResponseDto.from(savedRecord);
    }

    public RecordListResponseDto getRecordsByDate(LocalDate date, UserEntity user) {

        // 활성 스토리 조회 (IN_PROGRESS 또는 PENDING_LETTER)
        StoryEntity story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                .orElseGet(() -> storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER)
                        .orElseThrow(() -> new RuntimeException("현재 활성 스토리가 없습니다.")));

        // 해당 날짜의 레코드 조회
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        List<RecordEntity> records = recordRepository.findByStoryIdAndCreatedAtBetweenOrderByCreatedAtAscWithStory(
                story.getId(), startOfDay, endOfDay);

        if (records.isEmpty()) {
            throw new RuntimeException("레코드가 1건 이상 존재하지 않습니다.");
        }

        // DTO 변환
        List<RecordListResponseDto.RecordItemDto> recordItems = records.stream()
                .map(record -> {
                    RecordListResponseDto.RecordItemDto item = new RecordListResponseDto.RecordItemDto();
                    item.setId(record.getId());
                    item.setTime(record.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                    item.setTranscriptText(record.getTranscriptText());
                    item.setSummaryText(record.getSummaryText());
                    return item;
                })
                .collect(Collectors.toList());

        // 응답 구성
        RecordListResponseDto response = new RecordListResponseDto();
        response.setStoryId(story.getId());
        response.setTheme(story.getTheme());
        response.setColor(story.getColor());
        response.setRecords(recordItems);
        response.setTotalCount(recordItems.size());

        return response;
    }

    public RecordDetailResponseDto getRecordDetail(Long recordId, UserEntity user) {
        // 레코드 조회
        RecordEntity record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("레코드를 찾을 수 없습니다"));

        // 권한 확인 (해당 사용자의 스토리에 속한 레코드인지 확인)
        if (!record.getStory().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("권한이 없습니다");
        }

        return RecordDetailResponseDto.from(record);
    }

    public void deleteRecord(Long recordId, UserEntity user) {
        // 레코드 조회
        RecordEntity record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("레코드를 찾을 수 없습니다"));

        // 권한 확인 (해당 사용자의 스토리에 속한 레코드인지 확인)
        if (!record.getStory().getUser().getId().equals(user.getId())) {
            throw new RuntimeException("권한이 없습니다");
        }

        // 레코드 삭제
        recordRepository.delete(record);
    }
}