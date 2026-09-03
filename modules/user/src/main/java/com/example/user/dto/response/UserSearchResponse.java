package com.example.user.dto.response;

import com.example.user.entity.Role;
import com.example.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record UserSearchResponse(
        @JsonProperty("image_url")
        String imageUrl,

        String email,

        String name,

        String phone,

        String intro,

        Role role,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getImageUrl(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getIntro(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
