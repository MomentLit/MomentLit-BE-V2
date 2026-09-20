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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 매칭이 승인되어 특정 날짜에 이미 예약이 찬 공간을 표시한다 — `Matching`은 matching 모듈 소유라
 * space 모듈이 직접 조인할 수 없으므로(전 프로젝트 컨벤션: 모듈 간 JPA 연관관계 없음, 게다가
 * matching이 이미 space에 의존하는 방향이라 반대 의존은 순환이 됨), matching 모듈이 승인 시점에
 * {@code SpaceInternalApi.markSpaceBooked(...)}로 이 테이블에 직접 써준다. 날짜 검색
 * (`GET /spaces?date=`)은 이 테이블만 보고 이미 찬 날짜를 걸러낸다.
 *
 * 하루에 시간대가 겹치지 않는 여러 매칭이 각각 승인될 수 있어(예: 09-12시, 14-18시 슬롯)
 * (space_id, date) 유니크 제약은 두지 않는다 — 중복 행이 있어도 조회는 EXISTS로만 하므로 문제 없다.
 */
@Getter
@Entity
@Table(name = "space_booked_dates", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceBookedDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "matching_id", nullable = false)
    private Long matchingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SpaceBookedDate(Long spaceId, LocalDate date, Long matchingId) {
        this.spaceId = spaceId;
        this.date = date;
        this.matchingId = matchingId;
    }
}
