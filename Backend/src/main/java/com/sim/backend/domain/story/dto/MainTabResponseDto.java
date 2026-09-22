package com.sim.backend.domain.story.dto;

import com.sim.backend.domain.story.StoryEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MainTabResponseDto {
    private Long storyId;
    private String theme;
    private String color;
    private StoryEntity.StoryStatus status;
    private int daysFromStart;
    private int daysToEnd;
    private LocalDate date;
    private RetrospectDto retrospect;
    private List<RecordDto> records;
    private int recordsCount;

    @Getter
    @Setter
    public static class RetrospectDto {
        private Long id;
        private String title;
        private String content;
    }

    @Getter
    @Setter
    public static class RecordDto {
        private Long id;
        private String time;
        private String transcriptText;
        private String summaryText;
    }
}