package com.sim.backend.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

@JsonPropertyOrder({"status", "code", "message", "result"})
@Getter
public class ApiResponse<T> {

    private final int status;
    private final int code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    public ApiResponse(int status, int code, String message, T result) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public static <T> ApiResponse<T> onSuccess(int customCode, String message, T result) {
        // 성공 시 status: 200, code: 1000 (미리 약속된 성공 코드)
        return new ApiResponse<>(200, customCode, message, result);
    }

    // 데이터 없이 성공 응답을 생성하는 정적 메서드
    public static <T> ApiResponse<T> onSuccess(int customCode, String message) {
        return new ApiResponse<>(200, customCode, message,null);
    }

    public static <T> ApiResponse<T> onFailure(int httpStatus, int customCode, String message) {
        return new ApiResponse<>(httpStatus, customCode, message, null);
    }
}
