package com.example.room.global.client;

import com.example.room.global.exception.LayoutRejectedException;
import com.example.room.global.exception.LayoutUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * 방 구조 추론 서비스(RoomScan) 클라이언트. 내부 API 키로만 호출한다.
 * 추론은 CPU로 7~12초 걸리고 서비스가 한 번에 하나씩 처리해서, 읽기 타임아웃을 넉넉히 둔다.
 */
@Component
public class LayoutClient {

    private static final String DEFAULT_REJECT_MESSAGE = "사진에서 방 구조를 찾지 못했어요.";

    private final RestClient restClient;
    private final String internalApiKey;
    private final ObjectMapper objectMapper;

    public LayoutClient(
            @Value("${layout.base-url}") String baseUrl,
            @Value("${layout.internal-api-key}") String internalApiKey,
            ObjectMapper objectMapper
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.internalApiKey = internalApiKey;
        this.objectMapper = objectMapper;
    }

    /**
     * @param image  2:1 파노라마 바이트
     * @throws LayoutRejectedException    사진이 맞지 않거나 방 구조를 못 찾음 (사용자에게 그대로 보여 줄 메시지)
     * @throws LayoutUnavailableException 서비스가 응답하지 않거나 오류
     */
    public InferResult infer(byte[] image, boolean align, boolean cuboid) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(image) {
            @Override
            public String getFilename() {
                return "panorama.jpg";
            }
        });

        try {
            InferResult result = restClient.post()
                    .uri(uri -> uri.path("/infer")
                            .queryParam("align", align)
                            .queryParam("cuboid", cuboid)
                            .build())
                    .header("X-Internal-Api-Key", internalApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.value() == 400 || status.value() == 422, (request, response) -> {
                        throw new LayoutRejectedException(readDetail(response));
                    })
                    .body(InferResult.class);
            if (result == null || result.image() == null || result.uv() == null) {
                throw new LayoutUnavailableException("방 구조 추론 서비스가 빈 응답을 보냈습니다.");
            }
            return result;
        } catch (RestClientException e) {
            throw new LayoutUnavailableException("방 구조 추론 서비스를 호출하지 못했습니다: " + e.getMessage());
        }
    }

    /** FastAPI 오류 본문 {"detail": "..."}에서 메시지를 꺼낸다. */
    private String readDetail(ClientHttpResponse response) {
        try {
            JsonNode detail = objectMapper.readTree(response.getBody()).get("detail");
            return detail != null && detail.isString() ? detail.asString() : DEFAULT_REJECT_MESSAGE;
        } catch (IOException | RuntimeException e) {
            return DEFAULT_REJECT_MESSAGE;
        }
    }

    /** @param image 수평을 맞춘 JPEG (base64) */
    public record InferResult(
            String image,
            List<List<Double>> uv,
            double z0,
            double z1,
            boolean aligned
    ) {
    }
}
