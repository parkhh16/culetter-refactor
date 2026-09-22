package com.sim.backend.domain.letter;

import com.sim.backend.domain.letter.dto.LetterRequestDto;
import com.sim.backend.domain.letter.dto.LetterResponseDto;
import com.sim.backend.domain.story.StoryEntity;
import com.sim.backend.domain.story.StoryRepository;
import com.sim.backend.domain.users.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LetterService {

    private final StoryRepository storyRepository;
    private final LetterRepository letterRepository;

    @Transactional
    public LetterResponseDto saveLetter(LetterRequestDto requestDto, UserEntity user){
        // 현재 진행중인 스토리 또는 편지화 대기 중인 스토리 찾기
        StoryEntity story = storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.IN_PROGRESS)
                .orElseGet(() -> storyRepository.findByUserIdAndStatus(user.getId(), StoryEntity.StoryStatus.PENDING_LETTER)
                .orElseThrow(() -> new RuntimeException("활성 스토리가 없습니다")));

        LetterEntity letter = new LetterEntity();
        letter.setStory(story);
        letter.setTitle(requestDto.getTitle());
        letter.setContent(requestDto.getContent());
        letter.setStatus(LetterEntity.LetterStatus.FINAL);

        LetterEntity savedLetter = letterRepository.save(letter);

        story.setStatus(StoryEntity.StoryStatus.ENDED); //Dirty Checking으로 스토리가 완료되었다고 표시.

        return LetterResponseDto.from(savedLetter);
    }

    @Transactional(readOnly = true)
    public LetterResponseDto getLetterById(Long id, UserEntity user){
        Optional<LetterEntity> letterEntity = letterRepository.findById(id);

        if(letterEntity.isPresent()) {
            LetterEntity letter = letterEntity.get();
            StoryEntity storyEntity = storyRepository
                    .findById(letter.getStory().getId())
                    .get();

            UserEntity letterWrittenUser = storyEntity.getUser();
            if(letterWrittenUser.getId() != user.getId()) throw new RuntimeException("사용자가 작성한 편지가 아닙니다.");
            return LetterResponseDto.from(letterEntity.get());
        }
        else throw new RuntimeException("조회된 편지가 없습니다.");
    }

    @Transactional(readOnly = true)
    public List<LetterResponseDto> getLetterByUserId(Long id){
        List<LetterEntity> letterEntity = letterRepository.findByStory_User_Id(id);

        return letterEntity.stream()
                .map(LetterResponseDto::from)
                .toList();
    }
}
