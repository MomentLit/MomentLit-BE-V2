package com.example.auth.global.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfoResponse(
        Long id,

        @JsonProperty("kakao_account")
        KakaoAccount kakaoAccount
) {
    private static final String PROVIDER = "KAKAO";

    public record KakaoAccount(
            String email,

            @JsonProperty("is_email_valid")
            Boolean emailValid,

            @JsonProperty("is_email_verified")
            Boolean emailVerified,

            Profile profile
    ) {
    }

    public record Profile(
            String nickname,

            @JsonProperty("profile_image_url")
            String profileImageUrl
    ) {
    }

    public OauthUserProfile toProfile() {
        KakaoAccount account = kakaoAccount;
        Profile profile = account == null ? null : account.profile();

        return new OauthUserProfile(
                PROVIDER,
                id == null ? null : String.valueOf(id),
                account == null ? null : account.email(),
                account == null ? null : account.emailVerified(),
                profile == null ? null : profile.nickname(),
                profile == null ? null : profile.profileImageUrl()
        );
    }
}
