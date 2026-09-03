package com.example.popup.dto.response;

import com.example.popup.entity.Popup;
import com.example.space.dto.response.SpaceDetailResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PopupDetailResponse(
        @JsonProperty("popup_id")
        Long popupId,

        String title,

        String description,

        @JsonProperty("space_name")
        String spaceName,

        com.example.space.dto.response.AddressResponse address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("view_count")
        Integer viewCount,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("ai_brand_summary")
        String aiBrandSummary,

        @JsonProperty("start_time")
        LocalDateTime startTime,

        @JsonProperty("end_time")
        LocalDateTime endTime,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

    public static PopupDetailResponse from(
            Popup popup,
            SpaceDetailResponse space
    ) {
        return new PopupDetailResponse(
                popup.getId(),
                popup.getTitle(),
                popup.getDescription(),
                space.name(),
                space.address(),
                popup.getThumbnailUrl(),
                popup.getViewCount(),
                popup.getLikeCount(),
                popup.getAiBrandSummary(),
                popup.getStartTime(),
                popup.getEndTime(),
                popup.getCreatedAt()
        );
    }
}
