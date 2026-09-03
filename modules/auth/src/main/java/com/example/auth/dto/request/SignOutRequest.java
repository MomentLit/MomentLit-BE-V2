package com.example.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignOutRequest(
        @JsonProperty("refresh_token")
        String refreshToken
) {}
