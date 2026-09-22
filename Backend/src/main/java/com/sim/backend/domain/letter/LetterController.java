package com.sim.backend.domain.letter;

import com.google.api.Http;
import com.sim.backend.global.util.FirebaseTokenUtil;
import com.sim.backend.domain.letter.dto.LetterRequestDto;
import com.sim.backend.domain.letter.dto.LetterResponseDto;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/letters")
@RequiredArgsConstructor
public class LetterController {

    private final UserRepository userRepository;
    private final FirebaseTokenUtil firebaseTokenUtil;
    private final LetterService letterService;

    @PostMapping
    public ApiResponse<LetterResponseDto> saveLetter(@AuthenticationPrincipal UserEntity user,
                                                     @RequestBody LetterRequestDto requestDto){

        LetterResponseDto responseDto = letterService.saveLetter(requestDto, user);

        return ApiResponse.onSuccess(1000, "편지 저장 성공", responseDto);
    }

    @GetMapping("/{id}")
    public ApiResponse<LetterResponseDto> getLetterById(@AuthenticationPrincipal UserEntity user,
                                                        @PathVariable Long id){

        LetterResponseDto responseDto;

        try{
            responseDto = letterService.getLetterById(id, user);
        }
        catch(Exception e){
            if(e.getMessage().contains("조회된 편지가 없습니다.")){
                return ApiResponse.onFailure(500, 5000, "조회된 편지가 없습니다.");
            }
            if(e.getMessage().contains("사용자가 작성한 편지가 아닙니다.")){
                return ApiResponse.onFailure(500, 5001, "사용자가 작성한 편지가 아닙니다.");
            }
            return ApiResponse.onFailure(500, 9000, "내부 오류가 발생했습니다.");
        }

        return ApiResponse.onSuccess(1000, "편지 조회 성공", responseDto);
    }

    @GetMapping
    public ApiResponse<List<LetterResponseDto>> getLetters(@AuthenticationPrincipal UserEntity user){
        List<LetterResponseDto> responseDtos;

        try{
            responseDtos = letterService.getLetterByUserId(user.getId());
        }
        catch(Exception e){
            return ApiResponse.onFailure(500, 9000, "내부 오류가 발생했습니다.");
        }
        return ApiResponse.onSuccess(1000, "전체 편지 조회 성공.", responseDtos);
    }
}
