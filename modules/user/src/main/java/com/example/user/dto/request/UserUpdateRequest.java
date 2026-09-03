package com.example.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserUpdateRequest(
        String name,

        @JsonProperty("image_url")
        String imageUrl,

        String phone,

        String intro
) {
}
