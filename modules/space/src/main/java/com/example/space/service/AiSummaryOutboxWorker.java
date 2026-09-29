package com.example.space.service;

import com.example.space.dto.internal.AiSummaryRequest;
import com.example.space.global.client.ChatbotSummaryClient;
import com.example.space.global.client.ChatbotSyncClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSummaryOutboxWorker {

    private final AiSummaryOutboxService outboxService;
    private final ChatbotSummaryClient chatbotSummaryClient;
    private final ChatbotSyncClient chatbotSyncClient;

    @Value("${chatbot.ai-summary.batch-size:5}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${chatbot.ai-summary.fixed-delay-ms:5000}")
    public void processPendingSummaries() {
        for (int i = 0; i < batchSize; i++) {
            Optional<AiSummaryOutboxService.AiSummaryJob> job = outboxService.claimNext();
            if (job.isEmpty()) {
                return;
            }
            process(job.get());
        }
    }

    private void process(AiSummaryOutboxService.AiSummaryJob job) {
        try {
            Optional<AiSummaryRequest> request = outboxService.loadRequest(job);
            if (request.isEmpty()) {
                outboxService.discard(job);
                return;
            }

            String summary = chatbotSummaryClient.generate(request.get());
            if (outboxService.complete(job, summary)) {
                // 요약 저장은 이미 커밋됐다. 인덱스 갱신 실패가 요약 작업을 되돌리거나 재생성하지는 않는다.
                chatbotSyncClient.syncSpace(job.spaceId());
            }
        } catch (Exception exception) {
            log.warn("AI 공간 소개 생성 실패 (spaceId={}, version={}): {}",
                    job.spaceId(), job.summaryVersion(), exception.toString());
            outboxService.fail(job, exception);
        }
    }
}
