package com.example.alarm.dto.response;

import com.example.alarm.entity.Alarm;

public record AlarmResponse(
        Long id,
        Long matchingId,
        String description,
        boolean isRead
) {
    public static AlarmResponse from(Alarm alarm) {
        return new AlarmResponse(
                alarm.getId(),
                alarm.getMatchingId(),
                alarm.getDescription(),
                alarm.isRead()
        );
    }
}
