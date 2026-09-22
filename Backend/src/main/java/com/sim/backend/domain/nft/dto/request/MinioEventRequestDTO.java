package com.sim.backend.domain.nft.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MinioEventRequestDTO {

    @JsonProperty("Key")  // 루트의 Key 필드 매핑
    private String key;
}
