package com.example.review.dto.response;

import com.example.review.entity.PopupReview;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PopupReviewCreateResponse(
        @JsonProperty("popup_review_id")
        Long popupReviewId
) {

    public static PopupReviewCreateResponse from(PopupReview review) {
        return new PopupReviewCreateResponse(review.getId());
    }
}
