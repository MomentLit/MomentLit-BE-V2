package com.example.space.global.client;

import com.example.space.dto.internal.AiSummaryRequest;
import com.example.space.dto.internal.AiSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class ChatbotSummaryClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public ChatbotSummaryClient(
            @Value("${chatbot.base-url}") String baseUrl,
            @Value("${chatbot.internal-api-key}") String internalApiKey
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.internalApiKey = internalApiKey;
    }

    public String generate(AiSummaryRequest request) {
        AiSummaryResponse response = restClient.post()
                .uri("/internal/space-summaries")
                .header("X-Internal-Api-Key", internalApiKey)
                .body(request)
                .retrieve()
                .body(AiSummaryResponse.class);
        if (response == null || response.aiSummary() == null || response.aiSummary().isBlank()) {
            throw new IllegalStateException("챗봇이 빈 공간 소개를 반환했습니다.");
        }
        return response.aiSummary().trim();
    }
}
