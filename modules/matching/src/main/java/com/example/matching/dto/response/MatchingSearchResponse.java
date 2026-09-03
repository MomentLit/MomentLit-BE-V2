package com.example.matching.dto.response;

import com.example.matching.entity.Matching;
import com.example.matching.entity.MatchingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record MatchingSearchResponse(
        @JsonProperty("matching_id")
        Long matchingId,

        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("seller_id")
        String sellerId,

        @JsonProperty("host_id")
        String hostId,

        @JsonProperty("start_time")
        LocalDateTime startTime,

        @JsonProperty("end_time")
        LocalDateTime endTime,

        @JsonProperty("total_price")
        Integer totalPrice,

        MatchingStatus status,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

    public static MatchingSearchResponse from(Matching matching) {
        return new MatchingSearchResponse(
                matching.getId(),
                matching.getSpaceId(),
                matching.getSellerId(),
                matching.getHostId(),
                matching.getStartTime(),
                matching.getEndTime(),
                matching.getTotalPrice(),
                matching.getStatus(),
                matching.getCreatedAt()
        );
    }
}
