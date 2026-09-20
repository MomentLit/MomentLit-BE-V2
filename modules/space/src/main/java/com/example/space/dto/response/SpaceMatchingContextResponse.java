package com.example.space.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceMatchingContextResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("host_id")
        String hostId,

        boolean approved,

        boolean active,

        boolean available,

        /** null이면 호스트가 정원을 안 정한 것 — 이 경우 인원 제한을 검사하지 않는다. */
        Integer capacity
) {
}
