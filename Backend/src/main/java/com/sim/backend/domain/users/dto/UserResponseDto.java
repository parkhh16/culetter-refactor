package com.sim.backend.domain.users.dto;

import com.sim.backend.domain.users.UserEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {
    private Long id;
    private String firebaseUid;
    private String nickname;
    private String name;
    private String email;
    private String walletAddress;
    private Boolean aiVoice;
    private Boolean isSignUp;

    // Entity를 DTO로 변환하는 생성자
    public UserResponseDto(UserEntity user) {
        this.id = user.getId();
        this.firebaseUid = user.getFirebaseUid();
        this.nickname = user.getNickname();
        this.name = user.getName();
        this.email = user.getEmail();
        this.walletAddress = user.getWalletAddress();
        this.aiVoice = user.getAiVoice();
    }

    // Getters ...
}