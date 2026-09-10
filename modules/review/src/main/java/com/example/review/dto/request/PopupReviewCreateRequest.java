package com.example.review.dto.request;

import com.example.review.entity.VerificationType;

public record PopupReviewCreateRequest(
        Integer rating,
        String content,
        VerificationType verificationType,
        String verificationPayload
) {
}
