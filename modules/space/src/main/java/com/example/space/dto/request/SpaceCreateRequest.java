package com.example.space.dto.request;

import com.example.space.entity.SpaceCategory;
import com.example.space.entity.UsageUnit;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record SpaceCreateRequest(
        String name,

        String description,

        AddressRequest address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        @JsonProperty("panorama_url")
        String panoramaUrl,

        // 목록 순서대로 앞뒤 장면이 이어진다. 있으면 panorama_url 대신 첫 장면 URL을 저장한다.
        @JsonProperty("panorama_scenes")
        List<PanoramaSceneRequest> panoramaScenes,

        @Positive
        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        SpaceCategory category,

        String phone,

        @PositiveOrZero
        Double area,

        @PositiveOrZero
        Integer capacity,

        String floor,

        @JsonProperty("parking_info")
        String parkingInfo,

        @JsonProperty("usage_unit")
        UsageUnit usageUnit,

        @JsonProperty("is_draft")
        Boolean isDraft
) {
}
