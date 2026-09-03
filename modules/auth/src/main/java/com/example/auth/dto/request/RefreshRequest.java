package com.example.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefreshRequest(
        @JsonProperty("refresh_token")
        String refreshToken
) {
}
