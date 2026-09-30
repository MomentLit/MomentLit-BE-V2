package com.example.image.global.client;

import com.example.image.global.exception.PrivacyBlurFailedException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * 사진 속 사람 얼굴·차량 번호판 위치 검출 — Gemini(Interactions API)에 이미지를 보내 영역 좌표만 받는다.
 * 흐림 처리는 하지 않는다(PrivacyBlurService가 OpenCV로 처리).
 * 좌표는 box_2d = [ymin, xmin, ymax, xmax], 0~1000으로 정규화된 값.
 */
@Slf4j
@Component
public class GeminiDetectionClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    // 업로드 요청이 Gemini 응답을 무한정 기다리지 않도록 한다. 넘으면 실패로 보고 원본을 올린다.
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private static final String PROMPT = """
            Detect every human face and every vehicle license plate in this image,
            including small, distant, side-view and partially visible ones.
            Return a JSON array; each item has label ("face" or "license_plate")
            and box_2d as [ymin, xmin, ymax, xmax] normalized to 0-1000.
            Return [] if there are none.
            """;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "array",
            "items", Map.of(
                    "type", "object",
                    "properties", Map.of(
                            "label", Map.of("type", "string", "enum", List.of("face", "license_plate")),
                            "box_2d", Map.of("type", "array", "items", Map.of("type", "integer"))
                    ),
                    "required", List.of("label", "box_2d")
            )
    );

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiDetectionClient(
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.detection-model}") String model
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<DetectedRegion> detect(byte[] jpegBytes) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new PrivacyBlurFailedException("Gemini API 키가 설정되지 않았습니다.");
        }

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/interactions")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createRequestBody(jpegBytes))
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {
                    });
        } catch (RestClientResponseException e) {
            throw new PrivacyBlurFailedException(
                    "Gemini 호출 실패 (status=" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e
            );
        } catch (Exception e) {
            throw new PrivacyBlurFailedException("Gemini 호출 중 오류가 발생했습니다: " + e.getMessage(), e);
        }

        return parseRegions(extractText(response));
    }

    private Map<String, Object> createRequestBody(byte[] jpegBytes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", List.of(
                Map.of("type", "text", "text", PROMPT),
                Map.of(
                        "type", "image",
                        "mime_type", "image/jpeg",
                        "data", Base64.getEncoder().encodeToString(jpegBytes)
                )
        ));
        body.put("response_format", Map.of(
                "type", "text",
                "mime_type", "application/json",
                "schema", RESPONSE_SCHEMA
        ));
        // 좌표 검출에는 긴 추론이 필요 없어서 가장 낮은 단계로 둔다(이 모델은 minimal을 지원하지 않음).
        body.put("generation_config", Map.of("thinking_level", "low"));
        return body;
    }

    /** 응답의 steps 중 model_output 단계의 text를 꺼낸다. */
    private String extractText(Map<String, Object> response) {
        if (response == null || !"completed".equals(response.get("status"))) {
            throw new PrivacyBlurFailedException("Gemini 응답이 완료되지 않았습니다.");
        }

        if (response.get("steps") instanceof List<?> steps) {
            for (Object step : steps) {
                if (step instanceof Map<?, ?> stepMap
                        && "model_output".equals(stepMap.get("type"))
                        && stepMap.get("content") instanceof List<?> contents) {
                    for (Object content : contents) {
                        if (content instanceof Map<?, ?> contentMap
                                && contentMap.get("text") instanceof String text) {
                            return text;
                        }
                    }
                }
            }
        }

        throw new PrivacyBlurFailedException("Gemini 응답에 검출 결과가 없습니다.");
    }

    private List<DetectedRegion> parseRegions(String text) {
        List<Object> items;
        try {
            items = JsonParserFactory.getJsonParser().parseList(text);
        } catch (Exception e) {
            throw new PrivacyBlurFailedException("Gemini 검출 결과를 해석할 수 없습니다: " + text, e);
        }

        List<DetectedRegion> regions = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map
                    && map.get("label") instanceof String label
                    && map.get("box_2d") instanceof List<?> box
                    && box.size() == 4
                    && box.stream().allMatch(Number.class::isInstance)) {
                regions.add(new DetectedRegion(
                        label,
                        ((Number) box.get(0)).intValue(),
                        ((Number) box.get(1)).intValue(),
                        ((Number) box.get(2)).intValue(),
                        ((Number) box.get(3)).intValue()
                ));
            } else {
                log.warn("잘못된 Gemini 검출 항목을 무시합니다: {}", item);
            }
        }
        return regions;
    }

    /** 0~1000으로 정규화된 영역. */
    public record DetectedRegion(
            String label,
            int yMin,
            int xMin,
            int yMax,
            int xMax
    ) {
    }
}
