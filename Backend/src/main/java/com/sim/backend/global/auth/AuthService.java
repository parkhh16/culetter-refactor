package com.sim.backend.global.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.domain.users.dto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    @Transactional
    public UserResponseDto processLogin(String idToken) {
        UserResponseDto result;
        FirebaseToken decodedToken;
        try {
            decodedToken = firebaseAuth.verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            throw new SecurityException("Invalid Firebase ID token.", e);
        }

        String uid = decodedToken.getUid();

        // DB에서 uid로 사용자 조회
        Optional<UserEntity> userOptional = userRepository.findByFirebaseUid(uid);

        if (userOptional.isPresent()) {
            UserEntity user = userOptional.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            result = new UserResponseDto(user);
            result.setIsSignUp(false);
        } else {
            UserEntity user = new UserEntity();
            user.setFirebaseUid(uid);
            user.setEmail(decodedToken.getEmail());
            user.setName(decodedToken.getName());
            user.setAiVoice(false);
            userRepository.save(user);
            result = new UserResponseDto(user);
            result.setIsSignUp(true);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public UserEntity getUserByFirebaseToken(String idToken) throws FirebaseAuthException {
        String uid = firebaseAuth.verifyIdToken(idToken).getUid();
        UserEntity user = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
        return user;
    }

    public static Optional<UserEntity> getCurrentUser() {
        // 1. SecurityContextHolder에서 Authentication 객체를 가져옵니다.
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserEntity) {
            return Optional.of((UserEntity) principal);
        }

        return Optional.empty();
    }

}