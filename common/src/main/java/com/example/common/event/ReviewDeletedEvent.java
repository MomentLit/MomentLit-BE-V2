package com.example.common.event;

public record ReviewDeletedEvent(
        String reviewType,
        Long reviewId
) {
}
