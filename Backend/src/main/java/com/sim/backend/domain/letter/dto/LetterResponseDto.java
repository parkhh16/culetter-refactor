package com.sim.backend.domain.letter.dto;

import com.sim.backend.domain.letter.LetterEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LetterResponseDto {
    private Long id;
    private Long storyId;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LetterResponseDto from(LetterEntity letter){
        LetterResponseDto dto = new LetterResponseDto();
        dto.setId(letter.getId());
        dto.setStoryId(letter.getStory().getId());
        dto.setTitle(letter.getTitle());
        dto.setContent(letter.getContent());
        dto.setStatus(letter.getStatus().toString());
        dto.setCreatedAt(letter.getCreatedAt());
        dto.setUpdatedAt(letter.getUpdatedAt());
        return dto;
    }
}
