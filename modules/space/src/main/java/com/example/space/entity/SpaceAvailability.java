package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 열린 슬롯들을 시작 시각부터 차례로 이어 붙여 [startTime, endTime)을 빈틈 없이 덮는지.
     * 요청의 시작·종료 시각은 슬롯 경계와 정확히 맞아야 한다 — 슬롯 중간에서 시작하거나 끝나면 false.
     * 같은 시각에 시작하는 슬롯이 여러 개면 짧은 쪽을 따라가 경계를 잘게 맞춘다.
     */
    public static boolean coversConsecutively(
            List<SpaceAvailability> openSlots,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Map<LocalTime, LocalTime> endByStart = new HashMap<>();
        for (SpaceAvailability slot : openSlots) {
            endByStart.merge(slot.getStartTime(), slot.getEndTime(), (a, b) -> a.isBefore(b) ? a : b);
        }

        LocalTime cursor = startTime;
        while (cursor.isBefore(endTime)) {
            LocalTime next = endByStart.get(cursor);
            // 다음 슬롯이 없거나, 자정을 넘겨 0시로 돌아온 경우(무한 루프 방지)
            if (next == null || !next.isAfter(cursor)) {
                return false;
            }
            cursor = next;
        }
        return cursor.equals(endTime);
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
