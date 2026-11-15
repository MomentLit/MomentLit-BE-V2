package com.example.popup.dto.response;

import com.example.popup.entity.Popup;
import com.example.space.dto.response.AddressResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PopupListResponse(
        @JsonProperty("popup_id")
        Long popupId,

        String title,

        AddressResponse address,

        @JsonProperty("thumbnail_url")
        String thumbnailUrl,

        @JsonProperty("start_time")
        LocalDateTime startTime,

        @JsonProperty("end_time")
        LocalDateTime endTime,

        @JsonProperty("view_count")
        Integer viewCount,

        @JsonProperty("like_count")
        Integer likeCount
) {

    public static PopupListResponse from(
            Popup popup,
            AddressResponse address
    ) {
        return new PopupListResponse(
                popup.getId(),
                popup.getTitle(),
                address,
                popup.getThumbnailUrl(),
                popup.getStartTime(),
                popup.getEndTime(),
                popup.getViewCount(),
                popup.getLikeCount()
        );
    }
}
