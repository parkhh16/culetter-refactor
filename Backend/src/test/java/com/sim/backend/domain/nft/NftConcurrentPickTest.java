package com.sim.backend.domain.nft;

import com.sim.backend.domain.letter.LetterEntity;
import com.sim.backend.domain.letter.LetterRepository;
import com.sim.backend.domain.nft.entity.NftEntity;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import com.sim.backend.domain.users.UserRepository;
import com.sim.backend.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #3 NftQueryRepositoryImpl.pickAndClaimNft() 동시성 락 측정용 테스트.
 * 스레드를 띄우는 대신, 별도의 원시 JDBC 커넥션으로 특정 NFT 행을 미리 잠가 커밋하지 않은 채로 두고
 * pickAndClaimNft()를 호출했을 때 그 행을 건너뛰고 다른 NFT를 집는지(SKIP LOCKED) 확인한다.
 * 실제 스레드 타이밍을 맞출 필요가 없어 빠르고 결정적이다.
 * @DataJpaTest 기본값(테스트 전체를 하나의 트랜잭션으로 감싸고 끝에 롤백)을 쓰면 저장한 NFT가
 * 커밋되지 않아 별도 커넥션에서 아예 보이지 않으므로, NOT_SUPPORTED로 감싸는 트랜잭션을 없앤다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext
class NftConcurrentPickTest {

    @Autowired
    private NftRepository nftRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private LetterRepository letterRepository;
    @Autowired
    private DataSource dataSource;

    @Test
    void 다른_커넥션이_잠근_행은_건너뛰고_다른_NFT를_claim한다() throws Exception {
        UserEntity user = new UserEntity();
        user.setFirebaseUid("concurrent-test-uid");
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

        // 별도 커넥션에서 nft1 행을 FOR UPDATE로 잠그고 커밋하지 않는다 (다른 인스턴스가 처리 중인 상황 재현).
        try (Connection lockHolder = dataSource.getConnection()) {
            lockHolder.setAutoCommit(false);
            try (Statement stmt = lockHolder.createStatement()) {
                stmt.execute("SELECT * FROM nfts WHERE nft_id = " + nft1.getId() + " FOR UPDATE");
            }

            Optional<NftEntity> picked = nftRepository.pickAndClaimNft();

            assertThat(picked).isPresent();
            // 수정 전(락 없는 단순 SELECT): 잠긴 행 여부와 무관하게 그대로 nft1을 읽어 중복 픽 가능
            // 수정 후(FOR UPDATE SKIP LOCKED): 잠긴 nft1을 건너뛰고 nft2를 집는다 -> 중복 0건
            assertThat(picked.get().getId()).isEqualTo(nft2.getId());

            lockHolder.rollback();
        }
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
        NftEntity saved = nftRepository.save(nft);
        nftRepository.flush();
        return saved;
    }
}
