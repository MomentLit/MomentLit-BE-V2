package com.example.user.dto.response;

import com.example.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 다른 모듈이 "누군가의 이름+사진" 정도만 필요할 때 쓰는 최소 공개 프로필 — `UserNameResponse`는 건드리지 않고 새로 추가했다. */
public record UserProfileResponse(
        @JsonProperty("user_id")
        String userId,

        String name,

        @JsonProperty("image_url")
        String imageUrl
) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getImageUrl()
        );
    }
}
