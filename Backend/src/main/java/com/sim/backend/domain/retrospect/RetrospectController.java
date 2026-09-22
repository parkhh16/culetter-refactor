package com.sim.backend.domain.retrospect;

import com.sim.backend.global.util.FirebaseTokenUtil;
import com.sim.backend.domain.retrospect.dto.RetrospectRequestDto;
import com.sim.backend.domain.retrospect.dto.RetrospectResponseDto;
import com.sim.backend.domain.retrospect.dto.RetrospectAllResponseDto;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class RetrospectController {

    @Autowired
    private RetrospectService retrospectService;

    @Autowired
    private FirebaseTokenUtil firebaseTokenUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * 회고 생성 API
     * POST /api/stories/retrospects
     */
    @PostMapping("/api/stories/retrospects")
    public ResponseEntity<ApiResponse<RetrospectResponseDto>> createRetrospect(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody RetrospectRequestDto requestDto) {
        try {
            // content 필수 검증
            if (requestDto.getContent() == null || requestDto.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.onFailure(400, 4002, "content가 누락되었습니다.")
                );
            }

            RetrospectResponseDto retrospectResponse = retrospectService.createRetrospect(requestDto, user);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(1001, "회고 생성 성공", retrospectResponse)
            );

        } catch (RuntimeException e) {
            // 스토리를 찾을 수 없는 경우
            if (e.getMessage().contains("진행 중인 스토리를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 2003, "진행 중인 스토리를 찾을 수 없습니다.")
                );
            }
            // 중복 회고 에러
            if (e.getMessage().contains("중복 회고")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ApiResponse.onFailure(400, 4001, "중복 회고")
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

    /**
     * 회고 조회 API
     * GET /api/retrospects?date=YYYY-MM-DD
     */
    @GetMapping("/api/retrospects")
    public ResponseEntity<ApiResponse<RetrospectResponseDto>> getRetrospect(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            // date가 없으면 오늘 날짜 사용
            if (date == null) {
                date = LocalDate.now();
            }

            RetrospectResponseDto retrospectResponse = retrospectService.getRetrospectByDate(date, user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1000, "회고 조회 성공", retrospectResponse)
            );

        } catch (RuntimeException e) {
            // 스토리를 찾을 수 없는 경우
            if (e.getMessage().contains("진행 중인 스토리를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 2003, "진행 중인 스토리를 찾을 수 없습니다.")
                );
            }
            // 회고를 찾을 수 없는 경우
            if (e.getMessage().contains("해당 날짜에 작성된 회고가 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 4003, "해당 날짜에 작성된 회고가 없습니다.")
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

    /**
     * 전체 회고 조회 API
     * GET /api/retrospects/all
     */
    @GetMapping("/api/retrospects/all")
    public ResponseEntity<ApiResponse<RetrospectAllResponseDto>> getAllRetrospects(
            @AuthenticationPrincipal UserEntity user) {
        try {


            RetrospectAllResponseDto response = retrospectService.getAllRetrospects(user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1002, "스토리 회고 조회 성공", response)
            );

        } catch (RuntimeException e) {
            // 스토리를 찾을 수 없는 경우
            if (e.getMessage().contains("진행 중인 스토리를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 2003, "현재 활성 스토리가 없습니다.")
                );
            }
            // 회고가 없는 경우
            if (e.getMessage().contains("회고가 1건이상 존재하지 않습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 4003, "회고가 1건이상 존재하지 않습니다.")
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

    /**
     * 회고 삭제 API
     * DELETE /api/stories/retrospects/{retrospectId}
     */
    @DeleteMapping("/api/stories/retrospects/{retrospectId}")
    public ResponseEntity<ApiResponse<Void>> deleteRetrospect(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long retrospectId) {
        try {

            retrospectService.deleteRetrospect(retrospectId, user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1001, "회고 삭제 성공", null)
            );

        } catch (RuntimeException e) {
            // 회고를 찾을 수 없는 경우
            if (e.getMessage().contains("회고를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 4003, "회고 없음")
                );
            }
            // 권한이 없는 경우
            if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.onFailure(403, 7002, "권한 없음")
                );
            }
            // 기타 런타임 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러")
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러")
            );
        }
    }
}