package com.example.space.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * 요일×시간대 반복 가용시간 슬롯 하나.
 * new_FE의 ScheduleGrid(요일 7 × 시간대 3구간)가 통째로 덮어쓰는 단위와 대응된다.
 */
public record SpaceAvailabilitySlotRequest(
        @JsonProperty("day_of_week")
        DayOfWeek dayOfWeek,

        @JsonProperty("start_time")
        LocalTime startTime,

        @JsonProperty("end_time")
        LocalTime endTime,

        @JsonProperty("is_open")
        Boolean isOpen
) {
}
