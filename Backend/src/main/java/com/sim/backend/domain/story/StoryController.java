package com.sim.backend.domain.story;

import com.sim.backend.domain.story.dto.MainTabResponseDto;
import com.sim.backend.domain.story.dto.StoryRequestDto;
import com.sim.backend.domain.story.dto.StoryResponseDto;
import com.sim.backend.global.dto.ApiResponse;
import com.sim.backend.global.util.FirebaseTokenUtil;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/stories")
public class StoryController {

    @Autowired
    private StoryService storyService;

    @Autowired
    private FirebaseTokenUtil firebaseTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<StoryResponseDto>> createStory(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody StoryRequestDto requestDto) {
        try {
            // 입력 값 검증
            if (requestDto.getTheme() == null || requestDto.getTheme().trim().isEmpty() ||
                requestDto.getColor() == null || requestDto.getColor().trim().isEmpty() ||
                requestDto.getEndedAt() == null) {

                return ResponseEntity.badRequest().body(
                    ApiResponse.onFailure(400, 2000, "theme, color, endedAt는 필수 입력값입니다.")
                );
            }

            // 종료일 유효성 검증 (오늘 이후여야 함)
            LocalDate today = LocalDate.now();
            if (requestDto.getEndedAt().isBefore(today)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.onFailure(400, 2001, "종료일은 시작일과 같거나 이후여야 합니다.")
                );
            }

            StoryResponseDto storyResponse = storyService.createStory(requestDto, user);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(1001, "스토리 생성 성공", storyResponse)
            );

        } catch (RuntimeException e) {
            // 중복 스토리 생성 에러 처리
            if (e.getMessage().contains("이미 진행 중이거나 편지화 대기 중인 스토리가 있습니다")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.onFailure(409, 2004, "이미 진행 중인 스토리가 있습니다.")
                );
            }
            // 기타 런타임 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.")
            );
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MainTabResponseDto>> getMainTab(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        try {
            // date가 없으면 오늘 날짜 사용
            if (date == null) {
                date = LocalDate.now();
            }

            MainTabResponseDto mainTabResponse = storyService.getCurrentUserMainTab(date, user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1000, "메인탭 조회 성공", mainTabResponse)
            );

        } catch (RuntimeException e) {
            // StoryNotFoundException 처리
            if (e.getMessage().contains("활성 스토리가 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 2003, "진행 중인 스토리를 찾을 수 없습니다.")
                );
            }
            // 기타 런타임 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.")
            );
        }
    }
}