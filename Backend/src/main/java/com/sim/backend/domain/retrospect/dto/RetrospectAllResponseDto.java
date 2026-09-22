package com.sim.backend.domain.retrospect.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class RetrospectAllResponseDto {
    private Long storyId;
    private String theme;
    private String color;
    private List<RetrospectSimpleDto> retrospects;
    private int totalCount;

    @Getter
    @Setter
    public static class RetrospectSimpleDto {
        private LocalDate entryDate;
        private String title;
        private String content;

        public RetrospectSimpleDto(LocalDate entryDate, String title, String content) {
            this.entryDate = entryDate;
            this.title = title;
            this.content = content;
        }
    }
}