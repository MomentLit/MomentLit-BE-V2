package com.example.user.dto.response;

import com.example.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SignUpResponse(
        @JsonProperty("user_id")
        String userId
) {
    public static SignUpResponse from(User user) {
        return new SignUpResponse(user.getId());
    }
}
