package com.example.space.dto.request;

import com.example.space.entity.SpaceCategory;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SpaceCreateRequest(
        String name,

        String description,

        AddressRequest address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        SpaceCategory category,

        String phone
) {
}
