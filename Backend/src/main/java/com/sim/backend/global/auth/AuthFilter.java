package com.sim.backend.global.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class AuthFilter extends OncePerRequestFilter {

    // Firebase Auth와 UserRepository를 주입받습니다.
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String idToken = bearerToken.substring(7);

        try {
            // 2. Firebase Admin SDK를 사용해 ID 토큰을 검증하고 uid를 얻습니다.
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // 3. uid를 사용해 우리 DB에서 UserEntity를 조회합니다.
            UserEntity user = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

            // 4. Authentication 객체를 생성합니다.
            //    - 첫 번째 파라미터(principal): 바로 이 user 객체가 @AuthenticationPrincipal로 주입될 대상입니다.
            //    - 두 번째 파라미터(credentials): 보통 null로 둡니다.
            //    - 세 번째 파라미터(authorities): 사용자의 권한 목록입니다.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());

            // 5. SecurityContextHolder에 생성된 Authentication 객체를 저장합니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // 토큰 검증 또는 사용자 조회 실패 시 SecurityContext를 비웁니다.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}