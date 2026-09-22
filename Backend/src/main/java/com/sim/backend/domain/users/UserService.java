package com.sim.backend.domain.users;

import com.sim.backend.domain.users.dto.UserResponseDto;
import com.sim.backend.domain.users.dto.UserUpdateRequestDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponseDto updatePartialUser(String uid, UserUpdateRequestDto requestDto){
        Optional<UserEntity> userOptional = userRepository.findByFirebaseUid(uid);

        if(userOptional.isPresent()){
            UserEntity user = userOptional.get();

            if(requestDto.getNickname() != null){
                user.setNickname(requestDto.getNickname());
            }

            if(requestDto.getBirthdate() != null){
                user.setBirthdate(requestDto.getBirthdate());
            }

            if(requestDto.getWalletAddress() != null){
                user.setWalletAddress(requestDto.getWalletAddress());
            }

            if(requestDto.getAiVoice() != null){
                user.setAiVoice(requestDto.getAiVoice());
            }
        }
        return userOptional.map(UserResponseDto::new).orElse(null);
    }

    @Transactional
    public void deleteUser(String uid){
        userRepository.deleteByFirebaseUid(uid);
    }

    @Transactional
    public UserResponseDto getUserByEmail(String email){
        Optional<UserEntity> user = userRepository.findByEmail(email);
        return user
                .map(UserResponseDto::new)
                .orElse(null);
    }

}
