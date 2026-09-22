package com.sim.backend.domain.retrospect.dto;

import com.sim.backend.domain.retrospect.RetrospectEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class RetrospectResponseDto {

    private Long id;
    private Long storyId;
    private LocalDate entryDate;
    private String title;
    private String content;
    private String color;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;  // 조회 시에만 포함

    public static RetrospectResponseDto from(RetrospectEntity entity) {
        RetrospectResponseDto dto = new RetrospectResponseDto();
        dto.setId(entity.getId());
        dto.setStoryId(entity.getStory().getId());
        dto.setEntryDate(entity.getEntryDate());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setColor(entity.getStory().getColor());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}