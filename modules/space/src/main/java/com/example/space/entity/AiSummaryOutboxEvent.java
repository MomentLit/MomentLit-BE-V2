package com.example.space.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "space_ai_summary_outbox", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiSummaryOutboxEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "summary_version", nullable = false)
    private Long summaryVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiSummaryOutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AiSummaryOutboxEvent(Long spaceId, Long summaryVersion) {
        this.id = UUID.randomUUID().toString();
        this.spaceId = spaceId;
        this.summaryVersion = summaryVersion;
        this.status = AiSummaryOutboxStatus.PENDING;
        this.nextAttemptAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static AiSummaryOutboxEvent pending(Long spaceId, Long summaryVersion) {
        return new AiSummaryOutboxEvent(spaceId, summaryVersion);
    }

    public void claim() {
        this.status = AiSummaryOutboxStatus.PROCESSING;
        this.attemptCount++;
        this.lockedAt = LocalDateTime.now();
        this.updatedAt = this.lockedAt;
    }

    public void complete() {
        this.status = AiSummaryOutboxStatus.COMPLETED;
        this.lockedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void fail(String error, boolean retryable) {
        this.status = retryable ? AiSummaryOutboxStatus.PENDING : AiSummaryOutboxStatus.FAILED;
        this.lastError = error;
        this.lockedAt = null;
        this.nextAttemptAt = retryable ? LocalDateTime.now().plusSeconds(30L * attemptCount) : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
