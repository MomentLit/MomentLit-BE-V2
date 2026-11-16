package com.example.matching.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MatchingCreateRequest(
        @NotNull
        @JsonProperty("space_id")
        Long spaceId,

        @NotBlank
        @JsonProperty("start_time")
        String startTime,

        @NotBlank
        @JsonProperty("end_time")
        String endTime,

        // 선택 — 서버가 공간 요금으로 다시 계산하므로 값은 쓰지 않는다(예전 클라이언트 호환용).
        @JsonProperty("total_price")
        String totalPrice,

        @Positive
        @JsonProperty("guest_count")
        Integer guestCount
) {
}
