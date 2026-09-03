package com.example.space.dto.response;


import com.example.space.entity.SpaceSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ScheduleListResponses(
        List<ScheduleListResponse> schedules
) {

    public static ScheduleListResponses from(List<SpaceSchedule> schedules) {
        Map<LocalDate, List<SpaceSchedule>> groupedByDate = schedules.stream()
                .collect(Collectors.groupingBy(
                        schedule -> schedule.getStartTime().toLocalDate()
                ));

        List<ScheduleListResponse> responses = groupedByDate.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> ScheduleListResponse.from(
                        entry.getKey(),
                        entry.getValue()
                ))
                .toList();

        return new ScheduleListResponses(responses);
    }
}
