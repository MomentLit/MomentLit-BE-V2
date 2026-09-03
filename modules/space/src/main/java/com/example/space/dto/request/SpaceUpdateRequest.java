package com.example.space.dto.request;

import com.example.space.entity.SpaceCategory;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SpaceUpdateRequest(
        String name,

        String description,

        AddressRequest address,

        @JsonProperty("ai_summary")
        String aiSummary,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        SpaceCategory category,

        @Size(max=20)
        @Pattern(regexp = "^[0-9+(-\\\\)\\\\s]{7,20}$")
        String phone
) {
}
