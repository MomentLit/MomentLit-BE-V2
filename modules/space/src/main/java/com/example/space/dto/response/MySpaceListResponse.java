package com.example.space.dto.response;

import com.example.space.entity.Space;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MySpaceListResponse(
        @JsonProperty("space_id")
        Long spaceId,

        String name,

        AddressResponse address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("price_per_hour")
        Integer pricePerHour,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("admin_status")
        String adminStatus,

        @JsonProperty("is_active")
        Boolean isActive,

        String category,

        Double area,

        Integer capacity,

        String floor,

        @JsonProperty("parking_info")
        String parkingInfo,

        @JsonProperty("usage_unit")
        String usageUnit
) {

    public static MySpaceListResponse from(
            Space space,
            AddressResponse address
    ) {
        return new MySpaceListResponse(
                space.getId(),
                space.getName(),
                address,
                space.getThumbnailUrl(),
                space.getPricePerHour(),
                space.getLikeCount(),
                space.getAdminStatus().name(),
                space.getIsActive(),
                space.getCategory().name(),
                space.getArea(),
                space.getCapacity(),
                space.getFloor(),
                space.getParkingInfo(),
                space.getUsageUnit() != null ? space.getUsageUnit().name() : null
        );
    }
}
