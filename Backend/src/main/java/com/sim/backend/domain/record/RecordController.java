package com.sim.backend.domain.record;

import com.sim.backend.domain.record.dto.RecordRequestDto;
import com.sim.backend.domain.record.dto.RecordResponseDto;
import com.sim.backend.domain.record.dto.RecordListResponseDto;
import com.sim.backend.domain.record.dto.RecordDetailResponseDto;
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
public class RecordController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private FirebaseTokenUtil firebaseTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/api/stories/records")
    public ResponseEntity<ApiResponse<RecordResponseDto>> createRecord(
            @AuthenticationPrincipal UserEntity user,
            @RequestBody RecordRequestDto requestDto) {
        try {
            // 입력 값 검증
            if (requestDto.getTranscriptText() == null || requestDto.getTranscriptText().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.onFailure(400, 3001, "transcriptText는 필수입니다.")
                );
            }

            RecordResponseDto recordResponse = recordService.createRecord(requestDto, user);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.onSuccess(1001, "레코드 생성 성공", recordResponse)
            );

        } catch (RuntimeException e) {
            // 특정 비즈니스 로직 에러 처리
            if (e.getMessage().contains("활성 스토리가 없습니다") || e.getMessage().contains("활성 스토리")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 2003, "진행 중인 스토리를 찾을 수 없습니다.")
                );
            }
            // 기타 런타임 에러
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.")
            );
        } catch (Exception e) {
            e.printStackTrace(); // 디버깅용 로그 추가
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.onFailure(500, 9000, "내부 서버 에러입니다.")
            );
        }
    }

    @GetMapping("/api/records")
    public ResponseEntity<ApiResponse<RecordListResponseDto>> getRecords(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        try {
            // date가 없으면 오늘 날짜 사용
            if (date == null) {
                date = LocalDate.now();
            }

            RecordListResponseDto recordsResponse = recordService.getRecordsByDate(date, user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1002, "레코드 조회 성공", recordsResponse)
            );

        } catch (RuntimeException e) {
            // 활성 스토리가 없는 경우
            if (e.getMessage().contains("현재 활성 스토리가 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 2003, "현재 활성 스토리가 없습니다.")
                );
            }
            // 레코드가 없는 경우
            if (e.getMessage().contains("레코드가 1건 이상 존재하지 않습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 4003, "레코드가 1건 이상 존재하지 않습니다.")
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

    @GetMapping("/api/records/{recordId}")
    public ResponseEntity<ApiResponse<RecordDetailResponseDto>> getRecordDetail(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long recordId) {
        try {

            RecordDetailResponseDto recordDetail = recordService.getRecordDetail(recordId, user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1000, "레코드 상세 조회 성공", recordDetail)
            );

        } catch (RuntimeException e) {
            // 레코드를 찾을 수 없는 경우
            if (e.getMessage().contains("레코드를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 3003, "레코드를 찾을 수 없습니다")
                );
            }
            // 권한이 없는 경우
            if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.onFailure(403, 7002, "권한이 없습니다")
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

    @DeleteMapping("/api/records/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long recordId) {
        try {
            recordService.deleteRecord(recordId, user);

            return ResponseEntity.ok(
                ApiResponse.onSuccess(1001, "레코드 삭제 성공", null)
            );

        } catch (RuntimeException e) {
            // 레코드를 찾을 수 없는 경우
            if (e.getMessage().contains("레코드를 찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.onFailure(404, 3003, "레코드 없음")
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