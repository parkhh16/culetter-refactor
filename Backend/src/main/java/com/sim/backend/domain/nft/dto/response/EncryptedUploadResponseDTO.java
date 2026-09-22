package com.sim.backend.domain.nft.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EncryptedUploadResponseDTO {
    private String cid;
    private String ivB64;
    private String originalMime;
}
