package com.example.auth.global.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaverUserInfoResponse(
        Response response
) {
    private static final String PROVIDER = "NAVER";

    public record Response(
            String id,
            String email,
            String name,

            @JsonProperty("profile_image")
            String profileImage
    ) {
    }

    public OauthUserProfile toProfile() {
        if (response == null) {
            return new OauthUserProfile(PROVIDER, null, null, null, null, null);
        }

        return new OauthUserProfile(
                PROVIDER,
                response.id(),
                response.email(),
                null,
                response.name(),
                response.profileImage()
        );
    }
}
