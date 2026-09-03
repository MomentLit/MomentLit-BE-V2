package com.example.auth.global.client.dto.response;

public record OauthUserProfile(
        String provider,
        String providerId,
        String email,
        Boolean emailVerified,
        String name,
        String imageUrl
) {
}
