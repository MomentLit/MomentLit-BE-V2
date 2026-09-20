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

        @JsonProperty("host_name")
        String hostName,

        @JsonProperty("host_image_url")
        String hostImageUrl,

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

        @JsonProperty("like_count")
        Integer likeCount,

        String category,

        Double area,

        Integer capacity,

        String floor,

        @JsonProperty("parking_info")
        String parkingInfo,

        @JsonProperty("usage_unit")
        String usageUnit,

        @JsonProperty("admin_status")
        String adminStatus
) {

    public static SpaceDetailResponse from(
            Space space,
            AddressResponse address,
            List<SpaceImage> images,
            String hostName,
            String hostImageUrl
    ) {
        return new SpaceDetailResponse(
                space.getId(),
                space.getHostId(),
                hostName,
                hostImageUrl,
                space.getName(),
                space.getDescription(),
                space.getAiSummary(),
                address,
                space.getThumbnailUrl(),
                images.stream()
                        .map(SpaceImage::getImageUrl)
                        .toList(),
                space.getPricePerHour(),
                space.getLikeCount(),
                space.getCategory().name(),
                space.getArea(),
                space.getCapacity(),
                space.getFloor(),
                space.getParkingInfo(),
                space.getUsageUnit() != null ? space.getUsageUnit().name() : null,
                space.getAdminStatus().name()
        );
    }
}
