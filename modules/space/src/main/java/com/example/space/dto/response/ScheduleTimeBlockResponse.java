package com.example.space.dto.response;

import com.example.space.entity.SpaceSchedule;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record ScheduleTimeBlockResponse(
        @JsonProperty("schedule_id")
        Long scheduleId,

        @JsonProperty("start_time")
        LocalDateTime startTime,

        @JsonProperty("end_time")
        LocalDateTime endTime,

        String status
) {

    public static ScheduleTimeBlockResponse from(SpaceSchedule schedule) {
        return new ScheduleTimeBlockResponse(
                schedule.getId(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getIsBookable() ? "AVAILABLE" : "BLOCKED"
        );
    }
}
