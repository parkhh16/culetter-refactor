package com.sim.backend.domain.retrospect.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RetrospectRequestDto {

    private String title;      // nullable
    private String content;    // required
}