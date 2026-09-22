package com.sim.backend.domain.users.dto;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserUpdateRequestDto {

    // requestBody에서 적은 필드만 업데이트함
    @Nullable
    String nickname;

    @Nullable
    LocalDate birthdate;

    @Nullable
    String walletAddress;

    @Nullable
    Boolean aiVoice;
}
