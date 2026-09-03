package com.example.admin.dto.response;

import java.time.LocalDateTime;

public record AdminSpaceListResponse(
        Long spaceId,
        String hostId,
        String name,
        String description,
        String aiSummary,
        AddressResponse address,
        String thumbnailUrl,
        Integer pricePerHour,
        String adminStatus,
        Boolean isActive,
        String category,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminSpaceListResponse from(com.example.space.dto.response.AdminSpaceListResponse space){
        return new AdminSpaceListResponse(
                space.spaceId(),
                space.hostId(),
                space.name(),
                space.description(),
                space.aiSummary(),
                AddressResponse.from(space.address()),
                space.thumbnailUrl(),
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
