package com.example.like.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PopupLikeResponse(
        @JsonProperty("popup_id")
        Long popupId,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("is_liked")
        boolean liked
) {

    public static PopupLikeResponse liked(Long popupId, Integer likeCount) {
        return new PopupLikeResponse(popupId, likeCount, true);
    }

    public static PopupLikeResponse unliked(Long popupId, Integer likeCount) {
        return new PopupLikeResponse(popupId, likeCount, false);
    }
}
