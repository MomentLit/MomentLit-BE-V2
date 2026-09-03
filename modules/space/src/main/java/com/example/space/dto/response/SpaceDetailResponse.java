package com.example.space.dto.response;

import com.example.space.entity.Space;
import com.example.space.entity.SpaceImage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SpaceDetailResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("host_id")
        String hostId,

        String name,

        String description,

        @JsonProperty("ai_summary")
        String aiSummary,

        AddressResponse address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("image_urls")
        List<String> imageUrls,

        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        String category
) {

    public static SpaceDetailResponse from(
            Space space,
            AddressResponse address,
            List<SpaceImage> images
    ) {
        return new SpaceDetailResponse(
                space.getId(),
                space.getHostId(),
                space.getName(),
                space.getDescription(),
                space.getAiSummary(),
                address,
                space.getThumbnailUrl(),
                images.stream()
                        .map(SpaceImage::getImageUrl)
                        .toList(),
                space.getPricePerHour(),
                space.getCategory().name()
        );
    }
}
