package com.example.review.dto.request;

public record SpaceReviewCreateRequest(
        Integer rating,
        String content
) {
}
