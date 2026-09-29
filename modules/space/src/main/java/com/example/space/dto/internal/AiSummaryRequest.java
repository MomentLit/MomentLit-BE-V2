package com.example.space.dto.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiSummaryRequest(
        String name,
        String description,
        String category,
        String sido,
        String sigungu,
        Double area,
        Integer capacity,
        String floor,
        @JsonProperty("parking_info") String parkingInfo,
        @JsonProperty("usage_unit") String usageUnit,
        @JsonProperty("price_per_hour") Integer pricePerHour
) {
}
