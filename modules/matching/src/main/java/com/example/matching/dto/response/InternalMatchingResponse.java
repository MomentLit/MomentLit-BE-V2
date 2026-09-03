package com.example.matching.dto.response;

import com.example.matching.entity.Matching;
import com.example.matching.entity.MatchingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record InternalMatchingResponse(
        @JsonProperty("matching_id")
        Long matchingId,

        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("host_id")
        String hostId,

        @JsonProperty("seller_id")
        String sellerId,

        @JsonProperty("start_time")
        LocalDateTime startTime,

        @JsonProperty("end_time")
        LocalDateTime endTime,

        @JsonProperty("total_price")
        Integer totalPrice,

        MatchingStatus status,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {

    public static InternalMatchingResponse from(Matching matching) {
        return new InternalMatchingResponse(
                matching.getId(),
                matching.getSpaceId(),
                matching.getHostId(),
                matching.getSellerId(),
                matching.getStartTime(),
                matching.getEndTime(),
                matching.getTotalPrice(),
                matching.getStatus(),
                matching.getCreatedAt(),
                matching.getUpdatedAt()
        );
    }
}
