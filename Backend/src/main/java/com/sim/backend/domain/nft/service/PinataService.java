package com.sim.backend.domain.nft.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sim.backend.domain.letter.LetterEntity;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.global.util.CryptoUtil;
import com.sim.backend.global.util.KeyUtil;
import com.sim.backend.domain.nft.dto.response.EncryptedUploadResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import javax.annotation.Nullable;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PinataService {

    private final WebClient.Builder builder;
    private final WebClient pinataApiClient;
    private final NftRepository nftRepository;

    @Value("${pinata.gateway.base}")
    private String gatewayBase;

    public byte[] getBytes(String cid) {
        return builder.baseUrl(gatewayBase).build()
                .get().uri("/{p}", cid)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    private EncryptedUploadResponseDTO uploadEncryptedFile
            (String fileName, byte[] plaintext, byte[] aesKey, @Nullable String originalMime) throws Exception {

        byte[] iv = KeyUtil.newIv12();
        byte[] ciphertext = CryptoUtil.encryptAesGcm(plaintext, aesKey, iv);

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder.part("file", new ByteArrayResource(ciphertext) {
            @Override public String getFilename()
                { return fileName != null ? fileName : "enc.bin"; }
        }).contentType(MediaType.APPLICATION_OCTET_STREAM);

        JsonNode jsonNode = pinataApiClient.post()
                .uri("/pinning/pinFileToIPFS")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        String cid = jsonNode.path("IpfsHash").asText();
        String ivB64 = Base64.getEncoder().encodeToString(iv);

        return EncryptedUploadResponseDTO.builder()
                .cid(cid)
                .ivB64(ivB64)
                .originalMime(originalMime != null ? originalMime : MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .build();
    }

    private EncryptedUploadResponseDTO uploadEncryptedJson
            (String logicalNameWithoutExt, Object jsonObject, byte[] aesKey, @Nullable String mimeForProperty ) throws Exception {

        byte[] jsonBytes = new ObjectMapper().writeValueAsBytes(jsonObject);
        String fileName = (logicalNameWithoutExt != null ? logicalNameWithoutExt : "data") + ".json.enc";
        return uploadEncryptedFile(fileName, jsonBytes, aesKey,
                mimeForProperty != null ? mimeForProperty : "application/json");
    }

    private String uploadMetadataJson(String name, String description,
                                     EncryptedUploadResponseDTO audio, EncryptedUploadResponseDTO text) {
        Map<String, Object> metadata = Map.of(
                "name", name,
                "description", description,
                "audio", "ipfs://" + audio.getCid(),
                "text",  "ipfs://" + text.getCid(),
                "properties", Map.of(
                        "audio", Map.of(
                                "mime_type", audio.getOriginalMime(),
                                "iv_b64",    audio.getIvB64()
                        ),
                        "text", Map.of(
                                "mime_type", text.getOriginalMime(),
                                "iv_b64",    text.getIvB64()
                        )
                )
        );

        Map<String, Object> body = Map.of(
                "pinataContent", metadata,
                "pinataMetadata", Map.of("name", name + ".metadata.json"),
                "pinataOptions", Map.of("cidVersion", 1)
        );

        JsonNode res = pinataApiClient.post()
                .uri("/pinning/pinJSONToIPFS")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        return res.path("IpfsHash").asText();
    }

    public void encryptedUploadAndMakeMetadata
            (LetterEntity letter, String nftName, String description, byte[] aesKey, byte[] audioWavBytes, Object textJsonObject) throws Exception {
        
        var audio = uploadEncryptedFile(nftName + ".wav.enc", audioWavBytes, aesKey, "audio/wav");
        var text  = uploadEncryptedJson(nftName + ".text", textJsonObject, aesKey, "text/plain");

        String metadataCid = uploadMetadataJson(nftName, description, audio, text);

        nftRepository.setCidsByLetter(letter, text.getCid(), metadataCid, audio.getCid());
//        NftResponseDTO.builder()
//                .audioCid(audio.getCid()).audioIvB64(audio.getIvB64())
//                .textCid(text.getCid()).textIvB64(text.getIvB64())
//                .metadataCid(metadataCid)
//                .build();
    }
}
