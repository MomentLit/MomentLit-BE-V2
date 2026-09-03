package com.example.space.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.space.entity.Space;

public record SpaceListResponse(
        @JsonProperty("space_id")
        Long spaceId,

        String name,

        AddressResponse address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        String category
) {

    public static SpaceListResponse from(
            Space space,
            AddressResponse address
    ) {
        return new SpaceListResponse(
                space.getId(),
                space.getName(),
                address,
                space.getThumbnailUrl(),
                space.getPricePerHour(),
                space.getCategory().name()
        );
    }
}
