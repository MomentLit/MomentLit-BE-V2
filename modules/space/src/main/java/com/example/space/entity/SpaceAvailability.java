package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 호스트가 설정하는 "매주 반복되는 가용시간" 템플릿.
 * 특정 날짜에 걸리는 실제 예약 슬롯인 {@link SpaceSchedule}과는 역할이 분리된 별도 테이블이다.
 * (SpaceAvailability = 반복 템플릿, SpaceSchedule = 날짜별 실제 예약 인스턴스)
 */
@Getter
@Entity
@Table(name = "space_availabilities", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_open", nullable = false)
    private Boolean isOpen;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private SpaceAvailability(
            Long spaceId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Boolean isOpen
    ) {
        validate(spaceId, dayOfWeek, startTime, endTime);

        this.spaceId = spaceId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isOpen = isOpen != null ? isOpen : Boolean.TRUE;
    }

    public static SpaceAvailability create(
            Long spaceId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Boolean isOpen
    ) {
        return SpaceAvailability.builder()
                .spaceId(spaceId)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .isOpen(isOpen)
                .build();
    }

    private static void validate(
            Long spaceId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (spaceId == null) {
            throw new BadRequestException("공간 ID는 필수입니다.");
        }
        if (dayOfWeek == null) {
            throw new BadRequestException("요일은 필수입니다.");
        }
        if (startTime == null || endTime == null) {
            throw new BadRequestException("가용시간 시작/종료 시각은 필수입니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("가용시간 시작 시각은 종료 시각보다 빨라야 합니다.");
        }
    }
}
