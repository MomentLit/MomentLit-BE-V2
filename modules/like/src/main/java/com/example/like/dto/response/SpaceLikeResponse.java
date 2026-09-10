package com.example.like.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SpaceLikeResponse(
        @JsonProperty("space_id")
        Long spaceId,

        @JsonProperty("like_count")
        Integer likeCount,

        @JsonProperty("is_liked")
        boolean liked
) {

    public static SpaceLikeResponse liked(Long spaceId, Integer likeCount) {
        return new SpaceLikeResponse(spaceId, likeCount, true);
    }

    public static SpaceLikeResponse unliked(Long spaceId, Integer likeCount) {
        return new SpaceLikeResponse(spaceId, likeCount, false);
    }
}
