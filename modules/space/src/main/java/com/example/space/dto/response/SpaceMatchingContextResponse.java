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
        Integer capacity,

        /** 외부 홈페이지로 예약을 받는 공간이면 true — 모먼트릿 매칭 요청을 받지 않는다. */
        @JsonProperty("external_booking")
        boolean externalBooking
) {
}
