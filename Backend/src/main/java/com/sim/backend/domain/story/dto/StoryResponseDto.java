package com.sim.backend.domain.story.dto;

import com.sim.backend.domain.story.StoryEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class StoryResponseDto {
    private Long id;
    private String theme;
    private String color;
    private LocalDate startedAt;
    private LocalDate endedAt;
    private String status;
    private LocalDateTime createdAt;

    public static StoryResponseDto from(StoryEntity story) {
        StoryResponseDto dto = new StoryResponseDto();
        dto.setId(story.getId());
        dto.setTheme(story.getTheme());
        dto.setColor(story.getColor());
        dto.setStartedAt(story.getStartedAt());
        dto.setEndedAt(story.getEndedAt());
        dto.setStatus(story.getStatus().name());
        dto.setCreatedAt(story.getCreatedAt());
        return dto;
    }
}