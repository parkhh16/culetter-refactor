package com.sim.backend.domain.record.dto;

import com.sim.backend.domain.record.RecordEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RecordResponseDto {

    private Long id;
    private Long storyId;
    private String transcriptText;
    private String summaryText;
    private LocalDateTime createdAt;

    public static RecordResponseDto from(RecordEntity record) {
        RecordResponseDto dto = new RecordResponseDto();
        dto.setId(record.getId());
        dto.setStoryId(record.getStory().getId());
        dto.setTranscriptText(record.getTranscriptText());
        dto.setSummaryText(record.getSummaryText());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }
}