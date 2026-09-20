package com.example.space.dto.response;

import com.example.space.entity.SpaceAvailability;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record SpaceAvailabilityResponse(
        @JsonProperty("day_of_week")
        DayOfWeek dayOfWeek,

        @JsonProperty("start_time")
        LocalTime startTime,

        @JsonProperty("end_time")
        LocalTime endTime,

        @JsonProperty("is_open")
        Boolean isOpen
) {

    public static SpaceAvailabilityResponse from(SpaceAvailability availability) {
        return new SpaceAvailabilityResponse(
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getIsOpen()
        );
    }
}
