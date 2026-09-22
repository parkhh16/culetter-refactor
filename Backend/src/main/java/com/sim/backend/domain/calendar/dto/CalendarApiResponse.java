package com.sim.backend.domain.calendar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.sim.backend.global.dto.ApiResponse;
import lombok.Getter;

@JsonPropertyOrder({"status", "code", "message", "progress", "result"})
@Getter
public class CalendarApiResponse<T> extends ApiResponse<T> {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Integer progress;

    public CalendarApiResponse(int status, int code, String message, T result, Integer progress) {
        super(status, code, message, result);
        this.progress = progress;
    }

    public static <T> CalendarApiResponse<T> onSuccess(int code, String message, T result, Integer progress) {
        return new CalendarApiResponse<>(200, code, message, result, progress);
    }

    public static <T> CalendarApiResponse<T> onSuccess(int code, String message, T result) {
        return new CalendarApiResponse<>(200, code, message, result, null);
    }
}