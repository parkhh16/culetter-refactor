package com.sim.backend.global.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class FirebaseTokenUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractFirebaseUid(String idToken) {
        try {
            if (idToken == null || idToken.trim().isEmpty()) {
                throw new IllegalArgumentException("ID Token cannot be null or empty");
            }

            // Bearer 토큰에서 실제 토큰 부분만 추출
            String token = idToken;
            if (idToken.startsWith("Bearer ")) {
                token = idToken.substring(7);
            }

            return FirebaseAuth.getInstance()
                    .verifyIdToken(token)
                    .getUid();

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract Firebase UID from token", e);
        }
    }
}