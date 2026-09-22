package com.sim.backend.domain.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // firebaseUid는 더 이상 PK가 아니지만, 여전히 조회의 핵심 키이므로 이 메서드는 유지합니다.
    Optional<UserEntity> findByFirebaseUid(String firebaseUid);
    Optional<UserEntity> findByEmail(String email);
    void deleteByFirebaseUid(String firebaseUid);
}