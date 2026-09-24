package com.sim.backend.domain.calendar;

import com.sim.backend.domain.record.RecordRepository;
import com.sim.backend.domain.retrospect.RetrospectEntity;
import com.sim.backend.domain.retrospect.RetrospectRepository;
import com.sim.backend.domain.retrospect.dto.RetrospectResponseDto;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #1 CalendarService N+1 쿼리 측정용 테스트.
 * 스토리 3개 + 각 스토리당 회고 1개 상태에서 getCalendarList() 호출 시
 * 실제 실행되는 쿼리 수를 Hibernate Statistics로 카운트한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class CalendarServiceQueryCountTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private RetrospectRepository retrospectRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Test
    void getCalendarList_실행_쿼리_횟수() {
        UserEntity user = new UserEntity();
        user.setFirebaseUid("test-uid");
        userRepository.save(user);

        for (int i = 0; i < 3; i++) {
            StoryEntity story = new StoryEntity();
            story.setUser(user);
            story.setTheme("theme-" + i);
            story.setColor("#ffffff");
            story.setStartedAt(LocalDate.of(2026, 9, 1));
            story.setEndedAt(LocalDate.of(2026, 9, 30));
            story.setStatus(StoryEntity.StoryStatus.IN_PROGRESS);
            storyRepository.save(story);

            RetrospectEntity retrospect = new RetrospectEntity();
            retrospect.setStory(story);
            retrospect.setEntryDate(LocalDate.of(2026, 9, 10 + i));
            retrospect.setContent("content-" + i);
            retrospectRepository.save(retrospect);
        }

        em.flush();
        em.clear();

        CalendarService calendarService =
                new CalendarService(storyRepository, userRepository, retrospectRepository, recordRepository);

        Statistics statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<RetrospectResponseDto> result = calendarService.getCalendarList("test-uid", 2026, 9);

        long queryCount = statistics.getQueryExecutionCount();
        System.out.println("=== getCalendarList() 실행 쿼리 수: " + queryCount + " ===");

        assertThat(result).hasSize(3);
        // QueryDSL 조인 리팩토링 전: 5 (user 1 + story 목록 1 + 스토리별 회고 조회 N=3)
        // QueryDSL 조인 리팩토링 후: 2 (user 1 + story-retrospect join fetch 1, 스토리 수와 무관하게 고정)
        assertThat(queryCount).isEqualTo(2L);
    }
}
