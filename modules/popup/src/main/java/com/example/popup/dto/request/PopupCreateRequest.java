package com.example.popup.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PopupCreateRequest(
        @JsonProperty("matching_id")
        Long matchingId,

        String title,

        String description,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("start_time")
        String startTime,

        @JsonProperty("end_time")
        String endTime
) {
}
