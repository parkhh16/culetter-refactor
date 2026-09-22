package com.sim.backend.domain.letter.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LetterRequestDto {
    private String title;
    private String content;
}
