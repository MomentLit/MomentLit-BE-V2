package com.example.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignInResponse(
        String name,
        String role,
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("expires_in")
        Number expiresIn
) {
}
