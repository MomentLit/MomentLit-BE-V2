package com.example.space.service;

import com.example.space.dto.internal.AiSummaryRequest;
import com.example.space.entity.Address;
import com.example.space.entity.AiSummaryOutboxEvent;
import com.example.space.entity.AiSummaryOutboxStatus;
import com.example.space.entity.Space;
import com.example.space.repository.AddressRepository;
import com.example.space.repository.AiSummaryOutboxRepository;
import com.example.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 외부 LLM 호출 전후의 짧은 DB 작업만 트랜잭션으로 묶는다.
 * 따라서 네트워크 지연 동안 공간 생성/수정 트랜잭션이나 outbox 행 잠금을 유지하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AiSummaryOutboxService {

    private static final int MAX_ATTEMPTS = 3;

    private final AiSummaryOutboxRepository outboxRepository;
    private final SpaceRepository spaceRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public Optional<AiSummaryJob> claimNext() {
        LocalDateTime now = LocalDateTime.now();
        return outboxRepository.findClaimable(
                        AiSummaryOutboxStatus.PENDING,
                        AiSummaryOutboxStatus.PROCESSING,
                        now,
                        now.minusMinutes(2),
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(event -> {
                    event.claim();
                    return new AiSummaryJob(event.getId(), event.getSpaceId(), event.getSummaryVersion());
                });
    }

    @Transactional(readOnly = true)
    public Optional<AiSummaryRequest> loadRequest(AiSummaryJob job) {
        Space space = spaceRepository.findById(job.spaceId()).orElse(null);
        if (space == null || space.getDeletedAt() != null || !space.getAiSummaryVersion().equals(job.summaryVersion())) {
            return Optional.empty();
        }
        Address address = addressRepository.findById(space.getAddressId()).orElse(null);
        if (address == null) {
            return Optional.empty();
        }
        return Optional.of(new AiSummaryRequest(
                space.getName(),
                space.getDescription(),
                space.getCategory().name(),
                address.getSido(),
                address.getSigungu(),
                space.getArea(),
                space.getCapacity(),
                space.getFloor(),
                space.getParkingInfo(),
                space.getUsageUnit() == null ? null : space.getUsageUnit().name(),
                space.getPricePerHour()
        ));
    }

    /** @return 요약이 현재 버전에 반영되어 검색 인덱스 동기화가 필요한지 */
    @Transactional
    public boolean complete(AiSummaryJob job, String summary) {
        AiSummaryOutboxEvent event = outboxRepository.findById(job.eventId()).orElseThrow();
        event.complete();

        Space space = spaceRepository.findById(job.spaceId()).orElse(null);
        return space != null && space.getDeletedAt() == null && space.completeAiSummary(job.summaryVersion(), summary);
    }

    /** 삭제됐거나 더 최신 버전이 있는 작업은 외부 호출 없이 종료만 한다. */
    @Transactional
    public void discard(AiSummaryJob job) {
        outboxRepository.findById(job.eventId()).orElseThrow().complete();
    }

    @Transactional
    public void fail(AiSummaryJob job, Exception exception) {
        AiSummaryOutboxEvent event = outboxRepository.findById(job.eventId()).orElseThrow();
        boolean retryable = event.getAttemptCount() < MAX_ATTEMPTS;
        String error = exception.getClass().getSimpleName() + ": " + safeMessage(exception.getMessage());
        event.fail(error, retryable);

        if (!retryable) {
            spaceRepository.findById(job.spaceId())
                    .ifPresent(space -> space.failAiSummary(job.summaryVersion()));
        }
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "요약 생성 요청 실패";
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    public record AiSummaryJob(String eventId, Long spaceId, Long summaryVersion) {
    }
}
