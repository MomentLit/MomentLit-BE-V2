package com.example.space.dto.response;

import com.example.space.entity.SpaceBookedDate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record SpaceBookedDatesResponse(
        List<LocalDate> dates
) {

    public static SpaceBookedDatesResponse from(List<SpaceBookedDate> bookedDates) {
        return new SpaceBookedDatesResponse(
                bookedDates.stream()
                        .map(SpaceBookedDate::getDate)
                        .distinct()
                        .sorted(Comparator.naturalOrder())
                        .toList()
        );
    }
}
