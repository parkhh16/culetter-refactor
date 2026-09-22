package com.sim.backend.domain.record.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RecordListResponseDto {

    private Long storyId;
    private String theme;
    private String color;
    private List<RecordItemDto> records;
    private Integer totalCount;

    @Getter
    @Setter
    public static class RecordItemDto {
        private Long id;
        private String time;
        private String transcriptText;
        private String summaryText;
    }
}