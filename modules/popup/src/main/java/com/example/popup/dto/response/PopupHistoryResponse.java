package com.example.popup.dto.response;

import com.example.popup.entity.Popup;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PopupHistoryResponse(
        @JsonProperty("popup_id")
        Long popupId,

        String title,

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

    public static PopupHistoryResponse from(Popup popup) {
        return new PopupHistoryResponse(
                popup.getId(),
                popup.getTitle(),
                popup.getThumbnailUrl(),
                popup.getStartTime(),
                popup.getEndTime(),
                popup.getViewCount(),
                popup.getLikeCount()
        );
    }
}
