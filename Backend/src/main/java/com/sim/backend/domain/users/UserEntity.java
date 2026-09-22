package com.sim.backend.domain.users;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import jakarta.persistence.*;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT 설정
    private Long id;

    @Column(name = "firebase_uid", nullable = false, unique = true)
    private String firebaseUid;

    @Column(name = "google_id")
    private String googleId;

    @Column
    private String name;

    @Column // 스키마상 nullable이므로 @Column(nullable=true)가 기본값
    private String nickname;

    @Column
    private LocalDate birthdate; // DATE 타입은 LocalDate와 매핑

    @Column(unique = true)
    private String email;

    @Column(name = "wallet_address", unique = true) // 말씀하신대로 nullable로 설정 (기본값)
    private String walletAddress;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt; // DATETIME 타입은 LocalDateTime과 매핑

    @CreationTimestamp // 엔티티 생성 시 자동으로 현재 시간 저장
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 엔티티 업데이트 시 자동으로 현재 시간 저장
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ai_voice")
    private Boolean aiVoice;
}
