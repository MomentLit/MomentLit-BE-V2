package com.example.like.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PopupReviewLikeResponse(
        @JsonProperty("popup_review_id")
        Long popupReviewId,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("is_liked")
        boolean liked
) {

    public static PopupReviewLikeResponse liked(Long popupReviewId, Integer likeCount) {
        return new PopupReviewLikeResponse(popupReviewId, likeCount, true);
    }

    public static PopupReviewLikeResponse unliked(Long popupReviewId, Integer likeCount) {
        return new PopupReviewLikeResponse(popupReviewId, likeCount, false);
    }
}
