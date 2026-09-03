package com.example.space.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceMatchingContextResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("host_id")
        String hostId,

        boolean approved,

        boolean active,

        boolean available
) {
}
