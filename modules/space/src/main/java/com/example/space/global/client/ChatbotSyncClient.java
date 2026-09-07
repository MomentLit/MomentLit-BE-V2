package com.example.space.global.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ChatbotSyncClient {

    private final RestClient restClient;
    private final String internalApiKey;

    public ChatbotSyncClient(
            @Value("${chatbot.base-url}") String baseUrl,
            @Value("${chatbot.internal-api-key}") String internalApiKey
    ) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public void syncSpace(Long spaceId) {
        try {
            restClient.post()
                    .uri("/internal/spaces/{spaceId}/sync", spaceId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("챗봇 공간 동기화 실패 (spaceId={}): {}", spaceId, e.toString());
        }
    }
}
