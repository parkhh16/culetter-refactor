package com.sim.backend.domain.nft.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MintingRequestDTO {
    private Long letterId;
    private String receiverEmail;
    private LocalDateTime reservationDate;
}
