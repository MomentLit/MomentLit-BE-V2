package com.example.review.dto.response;

import com.example.review.entity.SpaceReview;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceReviewCreateResponse(
        @JsonProperty("space_review_id")
        Long spaceReviewId
) {

    public static SpaceReviewCreateResponse from(SpaceReview review) {
        return new SpaceReviewCreateResponse(review.getId());
    }
}
