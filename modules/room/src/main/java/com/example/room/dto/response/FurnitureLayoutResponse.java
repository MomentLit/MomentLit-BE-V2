package com.example.room.dto.response;

import com.example.room.dto.request.FurnitureLayoutRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record FurnitureLayoutResponse(
        @JsonProperty("space_id")
        Long spaceId,

        List<FurnitureLayoutRequest.Item> items,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {
}
