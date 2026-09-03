package com.example.matching.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

        @NotBlank
        @JsonProperty("total_price")
        String totalPrice
) {
}
