package com.example.user.dto.response;

import com.example.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserAuthResponse(
        @JsonProperty("user_id")
        String userId,

        String name,

        String role
) {
    public static UserAuthResponse from(User user) {
        return new UserAuthResponse(
                user.getId(),
                user.getName(),
                user.getRole().name()
        );
    }
}
