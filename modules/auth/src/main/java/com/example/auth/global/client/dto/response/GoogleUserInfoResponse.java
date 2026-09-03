package com.example.auth.global.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleUserInfoResponse(
        @JsonProperty("sub")
        String providerId,

        String email,

        @JsonProperty("email_verified")
        Boolean emailVerified,

        String name,

        @JsonProperty("picture")
        String imageUrl
) {
    private static final String PROVIDER = "GOOGLE";

    public OauthUserProfile toProfile() {
        return new OauthUserProfile(
                PROVIDER,
                providerId,
                email,
                emailVerified,
                name,
                imageUrl
        );
    }
}
