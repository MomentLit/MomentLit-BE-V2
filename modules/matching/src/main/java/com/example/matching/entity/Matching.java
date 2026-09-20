package com.example.matching.entity;

import com.example.matching.global.exception.ForbiddenException;
import com.example.matching.global.exception.InvalidMatchingStateException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
@Table(name = "matchings", schema = "matchings")
public class Matching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "space_id")
    private Long spaceId;

    @Column(nullable = false, name = "seller_id")
    private String sellerId;

    @Column(nullable = false, name = "host_id")
    private String hostId;

    @Column(nullable = false, name = "start_time")
    private LocalDateTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, name = "total_price")
    private Integer totalPrice;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchingStatus status;

    @CreationTimestamp
    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static Matching create(
            Long spaceId,
            String sellerId,
            String hostId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Integer totalPrice,
            Integer guestCount
    ) {
        return Matching.builder()
                .spaceId(spaceId)
                .sellerId(sellerId)
                .hostId(hostId)
                .startTime(startTime)
                .endTime(endTime)
                .totalPrice(totalPrice)
                .guestCount(guestCount)
                .status(MatchingStatus.REQUESTED)
                .build();
    }

    public void approve(String userId) {
        validateHost(userId);
        validateRequested();
        this.status = MatchingStatus.APPROVED;
    }

    public void reject(String userId) {
        validateHost(userId);
        validateRequested();
        this.status = MatchingStatus.REJECTED;
    }

    public void cancel(String userId) {
        validateSeller(userId);
        validateRequested();
        this.status = MatchingStatus.CANCELED;
    }

    public boolean isHost(String userId) {
        return hostId.equals(userId);
    }

    private void validateHost(String userId) {
        if (!isHost(userId)) {
            throw new ForbiddenException("매칭 처리 권한이 없습니다.");
        }
    }

    private void validateSeller(String userId) {
        if (!sellerId.equals(userId)) {
            throw new ForbiddenException("매칭 취소 권한이 없습니다.");
        }
    }

    private void validateRequested() {
        if (status != MatchingStatus.REQUESTED) {
            throw new InvalidMatchingStateException("승인 대기 상태인 예약 요청만 처리할 수 있습니다.");
        }
    }
}
