package com.sim.backend.domain.record.dto;

import com.sim.backend.domain.record.RecordEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class RecordDetailResponseDto {
    private Long id;
    private Long storyId;
    private String storyTheme;
    private String storyColor;
    private String transcriptText;
    private String summaryText;
    private LocalDate date;
    private String time;

    public static RecordDetailResponseDto from(RecordEntity record) {
        RecordDetailResponseDto dto = new RecordDetailResponseDto();
        dto.setId(record.getId());
        dto.setStoryId(record.getStory().getId());
        dto.setStoryTheme(record.getStory().getTheme());
        dto.setStoryColor(record.getStory().getColor());
        dto.setTranscriptText(record.getTranscriptText());
        dto.setSummaryText(record.getSummaryText());
        dto.setDate(record.getCreatedAt().toLocalDate());
        dto.setTime(record.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));

        return dto;
    }
}