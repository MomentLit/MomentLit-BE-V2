package com.example.space.dto.response;

import com.example.space.entity.SpaceAvailability;

import java.util.List;

public record SpaceAvailabilityListResponses(
        List<SpaceAvailabilityResponse> availabilities
) {

    public static SpaceAvailabilityListResponses from(List<SpaceAvailability> availabilities) {
        return new SpaceAvailabilityListResponses(
                availabilities.stream()
                        .map(SpaceAvailabilityResponse::from)
                        .toList()
        );
    }
}
