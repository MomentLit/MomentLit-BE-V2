package com.example.space.entity;

import com.example.space.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "space_schedules", schema = "spaces")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpaceSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "is_bookable", nullable = false)
    private Boolean isBookable;

    @Builder(access = AccessLevel.PRIVATE)
    private SpaceSchedule(
            Long spaceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean isBookable
    ) {
        validateTime(startTime, endTime);

        this.spaceId = spaceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isBookable = isBookable != null ? isBookable : true;
    }

    public static SpaceSchedule create(
            Long spaceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean isBookable
    ) {
        return SpaceSchedule.builder()
                .spaceId(spaceId)
                .startTime(startTime)
                .endTime(endTime)
                .isBookable(isBookable)
                .build();
    }

    public void update(
            LocalDateTime startTime,
            LocalDateTime endTime,
            Boolean isBookable
    ) {
        validateTime(startTime, endTime);

        this.startTime = startTime;
        this.endTime = endTime;

        if (isBookable != null) {
            this.isBookable = isBookable;
        }
    }

    private void validateTime(
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("일정 시작 시간과 종료 시간은 필수입니다.");
        }

        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("일정 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
    }
}
