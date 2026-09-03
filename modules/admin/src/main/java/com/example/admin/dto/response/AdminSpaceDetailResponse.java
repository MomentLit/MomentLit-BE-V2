package com.example.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSpaceDetailResponse(
        Long spaceId,
        String hostId,
        String name,
        String description,
        String aiSummary,
        AddressResponse address,
        String thumbnailUrl,
        List<String> imageUrls,
        Integer pricePerHour,
        String adminStatus,
        Boolean isActive,
        String category,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminSpaceDetailResponse from(com.example.space.dto.response.AdminSpaceDetailResponse space){
        return new AdminSpaceDetailResponse(
                space.spaceId(),
                space.hostId(),
                space.name(),
                space.description(),
                space.aiSummary(),
                AddressResponse.from(space.address()),
                space.thumbnailUrl(),
                space.imageUrls(),
                space.pricePerHour(),
                space.adminStatus(),
                space.isActive(),
                space.category(),
                space.phone(),
                space.createdAt(),
                space.updatedAt()
        );
    }
}
