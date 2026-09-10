package com.example.review.dto.response;

import com.example.review.entity.PopupReview;
import com.example.review.entity.VerificationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PopupReviewResponse(
        @JsonProperty("popup_review_id")
        Long popupReviewId,

        @JsonProperty("user_name")
        String userName,

        Integer rating,
        String content,

        @JsonProperty("verification_type")
        VerificationType verificationType,

        @JsonProperty("is_verified")
        Boolean isVerified,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

    public static PopupReviewResponse from(PopupReview review, String userName) {
        return new PopupReviewResponse(
                review.getId(),
                userName,
                review.getRating(),
                review.getContent(),
                review.getVerificationType(),
                review.getIsVerified(),
                review.getLikeCount(),
                review.getCreatedAt()
        );
    }
}
