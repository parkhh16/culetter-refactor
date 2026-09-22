package com.sim.backend.global.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.sim.backend.domain.users.dto.UserResponseDto;
import com.sim.backend.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ApiResponse<UserResponseDto> login(HttpServletRequest request) {

        String header = request.getHeader("Authorization");

        if(header == null || !header.startsWith("Bearer ")) return ApiResponse.onFailure(404,7001,"토큰 형식이 맞지 않습니다.");
        String idToken = header.substring(7);
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException e) {
            return ApiResponse.onFailure(500, 7000, "로그인에 실패했습니다.");
        }
        UserResponseDto response = authService.processLogin(idToken);
        if(response.getIsSignUp()) return ApiResponse.onSuccess(1001, "회원가입 성공", response);
        else return ApiResponse.onSuccess(1000,"로그인 성공.", response);

    }
}