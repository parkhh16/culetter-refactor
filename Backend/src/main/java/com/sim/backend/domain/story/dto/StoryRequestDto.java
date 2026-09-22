package com.sim.backend.domain.story.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StoryRequestDto {
    private String theme;
    private String color;
    private LocalDate endedAt;
}