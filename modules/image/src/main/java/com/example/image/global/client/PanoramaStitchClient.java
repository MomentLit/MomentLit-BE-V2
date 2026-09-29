package com.example.image.global.client;

import com.example.image.global.exception.PanoramaStitchFailedException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

/**
 * 360도 사진 합성 AI 서비스 호출 클라이언트.
 * 요청: POST {base-url}/internal/panoramas/stitch, multipart "files" (가이드 순서대로)
 * 응답: 합성된 등장방형(2:1) 이미지 바이너리, Content-Type은 image/jpeg | image/png | image/webp
 */
@Component
public class PanoramaStitchClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String internalApiKey;

    public PanoramaStitchClient(
            @Value("${panorama.base-url}") String baseUrl,
            @Value("${panorama.internal-api-key}") String internalApiKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.baseUrl = baseUrl;
        this.internalApiKey = internalApiKey;
    }

    public StitchedPanorama stitch(List<MultipartFile> files) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new PanoramaStitchFailedException("360도 사진 합성 서비스가 아직 설정되지 않았습니다.");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        files.forEach(file -> body.add("files", file.getResource()));

        ResponseEntity<byte[]> response;
        try {
            response = restClient.post()
                    .uri("/internal/panoramas/stitch")
                    .header("X-Internal-Api-Key", internalApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (Exception e) {
            throw new PanoramaStitchFailedException("360도 사진 합성 중 오류가 발생했습니다.", e);
        }

        byte[] bytes = response.getBody();
        MediaType contentType = response.getHeaders().getContentType();

        if (bytes == null || bytes.length == 0 || contentType == null) {
            throw new PanoramaStitchFailedException("360도 사진 합성 결과가 비어 있습니다.");
        }

        return new StitchedPanorama(bytes, contentType.getType() + "/" + contentType.getSubtype());
    }

    public record StitchedPanorama(
            byte[] bytes,
            String contentType
    ) {
    }
}
