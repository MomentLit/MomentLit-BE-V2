package com.example.space.dto.response;

import com.example.space.entity.UsageUnit;
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
        boolean externalBooking,

        /** null이면 최소 인원 제한 없음. */
        @JsonProperty("min_capacity")
        Integer minCapacity,

        /** 가격 문의 공간이면 null. DAILY 공간에서는 1일 요금이다. */
        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        @JsonProperty("price_inquiry")
        boolean priceInquiry,

        @JsonProperty("usage_unit")
        UsageUnit usageUnit,

        /** null이면 최소 이용 시간 제한 없음. */
        @JsonProperty("min_usage_hours")
        Integer minUsageHours,

        /** 인원 초과 요금 — 둘 다 null이면 초과 요금 없음. */
        @JsonProperty("base_guest_count")
        Integer baseGuestCount,

        @JsonProperty("extra_guest_fee")
        Integer extraGuestFee
) {
}
