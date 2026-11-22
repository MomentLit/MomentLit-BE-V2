package com.example.room.dto.response;

import com.example.room.entity.CustomFurniture;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.room.entity.CustomFurnitureMount;
import com.example.room.entity.CustomFurnitureSource;
import com.example.room.entity.CustomFurnitureStatus;

import java.time.LocalDateTime;

/**
 * 내 가구. type("u-{id}")을 3D 방 배치 항목의 type으로 쓴다.
 * status가 READY일 때만 modelUrl·thumbnailUrl이 있다(사진 생성 중이면 PROCESSING, 실패면 FAILED + failureReason).
 */
public record CustomFurnitureResponse(
        Long id,
        String type,
        String name,
        CustomFurnitureMount mount,
        CustomFurnitureSource source,
        CustomFurnitureStatus status,
        Double width,
        Double depth,
        Double height,
        @JsonProperty("model_url")
        String modelUrl,
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,
        @JsonProperty("failure_reason")
        String failureReason,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static CustomFurnitureResponse from(CustomFurniture furniture) {
        String assetPath = "/rooms/furniture/" + furniture.getId();
        return new CustomFurnitureResponse(
                furniture.getId(),
                furniture.type(),
                furniture.getName(),
                furniture.getMount(),
                furniture.getSource(),
                furniture.getStatus(),
                furniture.getWidth(),
                furniture.getDepth(),
                furniture.getHeight(),
                furniture.getModelUrl() == null ? null : assetPath + "/model",
                furniture.getThumbnailUrl() == null ? null : assetPath + "/thumbnail",
                furniture.getFailureReason(),
                furniture.getCreatedAt()
        );
    }
}
