package com.sim.backend.domain.letter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LetterRepository extends JpaRepository<LetterEntity, Long> {

    Optional<LetterEntity> findById(Long id);

    List<LetterEntity> findByStory_User_Id(Long userId);
}