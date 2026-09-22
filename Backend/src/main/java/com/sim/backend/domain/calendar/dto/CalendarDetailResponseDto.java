package com.sim.backend.domain.calendar.dto;

import com.sim.backend.domain.record.RecordEntity;
import com.sim.backend.domain.record.dto.RecordResponseDto;
import com.sim.backend.domain.retrospect.RetrospectEntity;
import com.sim.backend.domain.retrospect.dto.RetrospectResponseDto;
import lombok.Getter;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@Setter
public class CalendarDetailResponseDto {
    RetrospectResponseDto retrospect;
    private List<RecordDto> records;

    @Getter
    @Setter
    public static class RecordDto {
        private Long id;
        private String time;
        private String summaryText;

        public static RecordDto from(RecordEntity records){
            RecordDto dto = new RecordDto();
            dto.setId(records.getId());
            dto.setTime(records.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
            dto.setSummaryText(records.getSummaryText());
            return dto;
        }
    }
}
