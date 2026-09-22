package com.sim.backend.domain.nft.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class SentOrRecievedNftResponseDTO {
    private Long letterId;
    private LocalDateTime reservationDate;
    private String receiverEmail;
    private String title;
}