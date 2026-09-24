package com.sim.backend.domain.retrospect;

import java.time.LocalDate;
import java.util.List;

public interface RetrospectQueryRepository {
    List<RetrospectEntity> findByUserIdAndEntryDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
