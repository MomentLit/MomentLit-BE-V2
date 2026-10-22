package com.example.space.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 외부 예약 공간에서 "홈페이지에서 예약하기"를 누른 기록.
 * 예약 자체는 외부에서 일어나므로, 모먼트릿이 남길 수 있는 마지막 행동 데이터다.
 * 비로그인 사용자의 클릭은 user_id가 null이다.
 */
@Getter
@Entity
@Table(name = "space_external_booking_clicks", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceExternalBookingClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "user_id")
    private String userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SpaceExternalBookingClick(Long spaceId, String userId) {
        this.spaceId = spaceId;
        this.userId = userId;
    }
}
