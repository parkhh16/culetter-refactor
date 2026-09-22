package com.sim.backend.domain.nft.dto.request;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class RenderNftRequestDTO {
    private Long letterId;
}