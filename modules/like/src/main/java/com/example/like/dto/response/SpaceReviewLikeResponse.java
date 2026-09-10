package com.example.like.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceReviewLikeResponse(
        @JsonProperty("space_review_id")
        Long spaceReviewId,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("is_liked")
        boolean liked
) {

    public static SpaceReviewLikeResponse liked(Long spaceReviewId, Integer likeCount) {
        return new SpaceReviewLikeResponse(spaceReviewId, likeCount, true);
    }

    public static SpaceReviewLikeResponse unliked(Long spaceReviewId, Integer likeCount) {
        return new SpaceReviewLikeResponse(spaceReviewId, likeCount, false);
    }
}
