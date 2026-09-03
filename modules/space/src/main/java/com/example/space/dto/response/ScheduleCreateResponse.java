package com.example.space.dto.response;

import com.example.space.entity.SpaceSchedule;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ScheduleCreateResponse(
        @JsonProperty("schedule_id")
        Long scheduleId
) {

    public static ScheduleCreateResponse from(SpaceSchedule schedule) {
        return new ScheduleCreateResponse(schedule.getId());
    }
}
