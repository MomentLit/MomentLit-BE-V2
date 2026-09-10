package com.example.review.dto.response;

import com.example.review.entity.SpaceReview;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SpaceReviewResponse(
        @JsonProperty("space_review_id")
        Long spaceReviewId,

        @JsonProperty("user_name")
        String userName,

        Integer rating,
        String content,

        @JsonProperty("likes_count")
        Integer likesCount,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

    public static SpaceReviewResponse from(SpaceReview review, String userName) {
        return new SpaceReviewResponse(
                review.getId(),
                userName,
                review.getRating(),
                review.getContent(),
                review.getLikeCount(),
                review.getCreatedAt()
        );
    }
}
