package com.example.space.dto.response;

import com.example.space.entity.Space;
import com.example.space.entity.SpaceImage;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSpaceDetailResponse(
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

        @JsonProperty("admin_status")
        String adminStatus,

        @JsonProperty("is_active")
        Boolean isActive,

        String category,

        String phone,

        @JsonProperty("created_at")
        LocalDateTime createdAt,

        @JsonProperty("updated_at")
        LocalDateTime updatedAt
) {

    public static AdminSpaceDetailResponse from(
            Space space,
            AddressResponse address,
            List<SpaceImage> images
    ) {
        return new AdminSpaceDetailResponse(
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
                space.getAdminStatus().name(),
                space.getIsActive(),
                space.getCategory().name(),
                space.getPhone(),
                space.getCreatedAt(),
                space.getUpdatedAt()
        );
    }
}
