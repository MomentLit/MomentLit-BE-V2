package com.example.user.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserOauthRequest(
        String provider,

        @JsonProperty("provider_id")
        String providerId,

        String email,

        @JsonProperty("email_verified")
        Boolean emailVerified,

        String name,

        @JsonProperty("image_url")
        String imageUrl
) {
}
