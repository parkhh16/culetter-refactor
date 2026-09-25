package com.sim.backend.domain.nft;

import com.sim.backend.domain.letter.LetterEntity;
import com.sim.backend.domain.letter.LetterRepository;
import com.sim.backend.domain.nft.controller.NftController;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.domain.nft.service.NftService;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * #2 NFT 스케줄러 트랜잭션 경계 측정용 테스트.
 * NFT 3건 중 3번째 처리에서 예외가 나도, 앞서 성공한 1·2번째 건이 롤백되지 않고
 * 독립적으로 COMPLETED까지 커밋되는지 검증한다.
 * 테스트 전체를 하나의 트랜잭션으로 감싸면(@DataJpaTest 기본값) 리포지토리의 개별
 * @Transactional 호출이 전부 그 안에 합류해버려 수정 효과가 가려지므로,
 * NOT_SUPPORTED로 감싸는 트랜잭션 자체를 없애 각 리포지토리 호출이 실제로 독립 커밋되게 한다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
class NftSchedulerTransactionBoundaryTest {

    @Autowired
    private NftRepository nftRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private LetterRepository letterRepository;

    @Test
    @Timeout(10)
    void 세번째_NFT_처리가_실패해도_앞선_두건은_COMPLETED로_남는다() throws Exception {
        UserEntity user = new UserEntity();
        user.setFirebaseUid("scheduler-test-uid");
        userRepository.save(user);

        StoryEntity story = new StoryEntity();
        story.setUser(user);
        story.setTheme("theme");
        story.setColor("#ffffff");
        story.setStartedAt(LocalDate.of(2026, 9, 1));
        story.setEndedAt(LocalDate.of(2026, 9, 30));
        story.setStatus(StoryEntity.StoryStatus.IN_PROGRESS);
        storyRepository.save(story);

        NftEntity nft1 = saveReadyNft(user, story, 1);
        NftEntity nft2 = saveReadyNft(user, story, 2);
        NftEntity nft3 = saveReadyNft(user, story, 3);

        NftService mockNftService = mock(NftService.class);
        doAnswer(invocation -> {
            NftEntity arg = invocation.getArgument(0);
            if (arg.getId().equals(nft3.getId())) {
                throw new IOException("simulated blockchain failure on 3rd NFT");
            }
            return null;
        }).when(mockNftService).mintToSmartContract(any());

        NftController controller = new NftController(nftRepository, mockNftService);
        controller.mintToSmartContract();

        List<NftEntity> all = nftRepository.findAllById(List.of(nft1.getId(), nft2.getId(), nft3.getId()));
        long completedCount = all.stream()
                .filter(n -> n.getStatus() == NftEntity.NftStatus.COMPLETED)
                .count();

        // 수정 전(스케줄러 전체가 하나의 @Transactional): 3번째 실패 시 예외가 메서드 밖으로 던져지며
        //   1, 2번째의 COMPLETED 갱신까지 전부 롤백 -> 커밋된 건수 0
        // 수정 후(NFT 1건 = 독립 트랜잭션, 실패 건만 개별 처리): 1, 2번째는 COMPLETED로 커밋되어 남음 -> 2
        assertThat(completedCount).isEqualTo(2L);

        NftEntity reloadedThird = nftRepository.findById(nft3.getId()).orElseThrow();
        // 실패한 3번째는 MINT_FAILED로 남아 이번 실행에서는 재시도되지 않는다(무한루프 방지).
        // 다음 스케줄 실행 시작 시 resetFailedForRetry()가 READY_TO_MINT로 되돌린다.
        assertThat(reloadedThird.getStatus()).isEqualTo(NftEntity.NftStatus.MINT_FAILED);
    }

    private NftEntity saveReadyNft(UserEntity user, StoryEntity story, int seq) {
        LetterEntity letter = LetterEntity.builder()
                .story(story)
                .title("title-" + seq)
                .content("content-" + seq)
                .status(LetterEntity.LetterStatus.FINAL)
                .build();
        letterRepository.save(letter);

        NftEntity nft = NftEntity.builder()
                .sender(user)
                .receiverEmail("receiver" + seq + "@test.com")
                .reservationDate(LocalDateTime.of(2026, 9, 20, 0, seq))
                .letter(letter)
                .status(NftEntity.NftStatus.READY_TO_MINT)
                .build();
        return nftRepository.save(nft);
    }
}
