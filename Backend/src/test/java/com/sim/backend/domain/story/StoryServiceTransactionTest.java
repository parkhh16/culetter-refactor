package com.sim.backend.domain.story;

import com.sim.backend.domain.record.RecordRepository;
import com.sim.backend.domain.retrospect.RetrospectRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * #4 StoryService.getCurrentUserMainTab() 트랜잭션 경계 측정용 테스트.
 * 이 메서드는 "기간 종료된 스토리를 PENDING_LETTER로 갱신 -> 이후 레코드 조회"를 한 흐름으로 처리하는데,
 * 뒤쪽 레코드 조회에서 예외가 나도 앞선 상태 갱신이 롤백되지 않으면 상태 불일치가 남는다.
 * @DataJpaTest 기본값(테스트 전체를 하나의 트랜잭션으로 감싸고 끝에 롤백)을 쓰면 서비스의
 * @Transactional이 그 안에 합류해버려 "즉시 롤백됐는지"를 확인할 수 없으므로, NOT_SUPPORTED로
 * 감싸는 트랜잭션 자체를 없애 서비스 메서드가 자기 트랜잭션을 온전히 소유하게 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({StoryService.class, QueryDslConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
class StoryServiceTransactionTest {

    @Autowired
    private StoryService storyService;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private UserRepository userRepository;
    @MockBean
    private RecordRepository recordRepository;
    @Autowired
    private RetrospectRepository retrospectRepository;

    @Test
    void 뒤쪽_조회가_실패하면_앞선_상태갱신도_롤백된다() {
        UserEntity user = new UserEntity();
        user.setFirebaseUid("story-tx-test-uid");
        userRepository.save(user);

        StoryEntity story = new StoryEntity();
        story.setUser(user);
        story.setTheme("theme");
        story.setColor("#ffffff");
        story.setStartedAt(LocalDate.of(2026, 8, 1));
        story.setEndedAt(LocalDate.of(2026, 8, 31)); // 이미 종료된 기간
        story.setStatus(StoryEntity.StoryStatus.IN_PROGRESS);
        StoryEntity saved = storyRepository.save(story);

        // 레코드 조회 단계에서 예외를 던지도록 만들어, 상태 갱신 이후 실패하는 상황을 재현한다.
        when(recordRepository.findByStoryIdAndCreatedAtBetweenOrderByCreatedAtAscWithStory(any(), any(), any()))
                .thenThrow(new RuntimeException("simulated record lookup failure"));

        assertThatThrownBy(() ->
                storyService.getCurrentUserMainTab(LocalDate.of(2026, 9, 1), user)
        ).isInstanceOf(RuntimeException.class);

        StoryEntity reloaded = storyRepository.findById(saved.getId()).orElseThrow();
        // 수정 전(@Transactional 없음): story.save()가 즉시 커밋되어 PENDING_LETTER로 남는다.
        // 수정 후(@Transactional 적용): 이후 예외로 전체 트랜잭션이 롤백되어 IN_PROGRESS 그대로 남는다.
        assertThat(reloaded.getStatus()).isEqualTo(StoryEntity.StoryStatus.IN_PROGRESS);
    }
}
