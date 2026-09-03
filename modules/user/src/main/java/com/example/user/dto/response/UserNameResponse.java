package com.example.user.dto.response;

import com.example.user.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record UserNameResponse(
        @JsonProperty("user_id")
        String userId,

        String name
) {

    public static UserNameResponse from(User user) {
        return new UserNameResponse(
                user.getId(),
                user.getName()
        );
    }
}
