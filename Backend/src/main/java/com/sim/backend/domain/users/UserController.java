package com.sim.backend.domain.users;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.sim.backend.domain.users.dto.UserResponseDto;
import com.sim.backend.domain.users.dto.UserUpdateRequestDto;
import com.sim.backend.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @PatchMapping("/me")
    public ApiResponse<UserResponseDto> updatedCurrentUser(@AuthenticationPrincipal UserEntity user,
                                                @RequestBody UserUpdateRequestDto requestDto){

        UserResponseDto dto = userService.updatePartialUser(user.getFirebaseUid(), requestDto);
        return ApiResponse.onSuccess(1000, "업데이트 성공", dto);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteCurrentUser(@AuthenticationPrincipal UserEntity user){
        userService.deleteUser(user.getFirebaseUid());
        return ApiResponse.onSuccess(1000, "회원 탈퇴 완료");
    }

    @GetMapping
    public ApiResponse<UserResponseDto> getUserByEmail(@RequestParam String email){
        try{
            UserResponseDto dto = userService.getUserByEmail(email);
            if(dto == null) return ApiResponse.onSuccess(1003, "일치하는 회원 없음");
            return ApiResponse.onSuccess(1000, "회원 조회 완료", dto);
        }
        catch(Exception e){
            return ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.");
        }
    }
}
