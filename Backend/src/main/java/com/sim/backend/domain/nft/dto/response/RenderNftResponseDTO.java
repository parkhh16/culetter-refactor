package com.sim.backend.domain.nft.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RenderNftResponseDTO {
//     이미지 파일 (암호화 X)
//    private String imageData;

    // 텍스트 원문 (암호화 X)
    private String title;
    private String content;

    // 음성 파일 (암호화 X)
    private String audioData;
}