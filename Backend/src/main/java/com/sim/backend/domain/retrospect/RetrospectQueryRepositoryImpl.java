package com.sim.backend.domain.retrospect;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sim.backend.domain.story.QStoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RetrospectQueryRepositoryImpl implements RetrospectQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RetrospectEntity> findByUserIdAndEntryDateBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        QRetrospectEntity retrospect = QRetrospectEntity.retrospectEntity;
        QStoryEntity story = QStoryEntity.storyEntity;

        return queryFactory
                .selectFrom(retrospect)
                .join(retrospect.story, story).fetchJoin()
                .where(
                        story.user.id.eq(userId),
                        retrospect.entryDate.between(startDate, endDate)
                )
                .orderBy(retrospect.entryDate.asc())
                .fetch();
    }
}
