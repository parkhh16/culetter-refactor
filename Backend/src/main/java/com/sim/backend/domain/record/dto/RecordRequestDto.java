package com.sim.backend.domain.record.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecordRequestDto {

    private String transcriptText;
    private String summaryText;
}