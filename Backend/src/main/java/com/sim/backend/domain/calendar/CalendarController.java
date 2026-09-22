package com.sim.backend.domain.calendar;

import com.sim.backend.global.util.FirebaseTokenUtil;
import com.sim.backend.domain.calendar.dto.CalendarApiResponse;
import com.sim.backend.domain.calendar.dto.CalendarDetailResponseDto;
import com.sim.backend.domain.retrospect.RetrospectService;
import com.sim.backend.domain.retrospect.dto.RetrospectResponseDto;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final FirebaseTokenUtil firebaseTokenUtil;
    private final CalendarService calendarService;
    private final RetrospectService retrospectService;
    private final UserRepository userRepository;

    @GetMapping
    public Object getCalenderInfo(@AuthenticationPrincipal UserEntity user,
                                  @RequestParam(required = false) Integer year,
                                  @RequestParam(required = false) Integer month) {

        try {
            if (year == null) {
                year = LocalDate.now().getYear();
            }

            if (month == null) {
                month = LocalDate.now().getMonthValue();
            }

            List<RetrospectResponseDto> calendarList = calendarService.getCalendarList(user.getFirebaseUid(), year, month);
            StoryEntity story = calendarService.getActiveStory(user.getFirebaseUid());
            int progress = calendarService.calculateProgress(story, calendarList);

            return CalendarApiResponse.onSuccess(1000, "조회 성공", calendarList, progress);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.onFailure(404, 9000, "유효하지 않은 요청입니다.");
        }
    }

    @GetMapping("/detail")
    public ApiResponse<CalendarDetailResponseDto> getCalendarDetail(@AuthenticationPrincipal UserEntity user,
                                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        CalendarDetailResponseDto responseDto;
        try {
            RetrospectResponseDto retrospectDto = retrospectService.getRetrospectByDate(date, user);
            responseDto = calendarService.getCalendarDetail(user.getFirebaseUid(), date, retrospectDto);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.onFailure(404, 9000, "유효하지 않은 요청입니다.");
        }

        return ApiResponse.onSuccess(1000, "조회 성공", responseDto);
    }
}
