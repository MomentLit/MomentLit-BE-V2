package com.example.space.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExternalBookingClickCountResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("total_clicks")
        long totalClicks,

        @JsonProperty("last_30_days_clicks")
        long last30DaysClicks
) {
}
