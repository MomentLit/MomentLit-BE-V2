package com.example.auth.service;

import com.example.auth.dto.request.RefreshRequest;
import com.example.auth.dto.request.SignInRequest;
import com.example.auth.dto.request.SignOutRequest;
import com.example.auth.global.client.dto.response.OauthUserProfile;
import com.example.auth.global.exception.BadRequestException;
import com.example.auth.global.exception.UnauthorizedException;
import com.example.user.dto.response.UserAuthResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthValidator {
    public void validateLoginRequest(SignInRequest request) {
        if (request == null || !StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new BadRequestException("아이디와 비밀번호를 입력해주세요.");
        }
    }

    public void validateRefreshRequest(RefreshRequest request) {
        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new BadRequestException("Refresh Token을 입력해주세요.");
        }
    }

    public void validateGoogleAuthorizationCode(String code) {
        validateOauthAuthorizationCode(code, "Google");
    }

    public void validateOauthAuthorizationCode(String code, String providerName) {
        if (!StringUtils.hasText(code)) {
            throw new BadRequestException(providerName + " Authorization Code를 입력해주세요.");
        }
    }

    public void validateOauthProfile(OauthUserProfile profile, String providerName) {
        if (profile == null
                || !StringUtils.hasText(profile.provider())
                || !StringUtils.hasText(profile.providerId())) {
            throw new BadRequestException(providerName + " 사용자 정보를 확인할 수 없습니다.");
        }

        if (!StringUtils.hasText(profile.email())) {
            throw new BadRequestException(providerName + " 이메일 제공 동의가 필요합니다.");
        }
    }

    public void validateSignOutRequest(SignOutRequest request) {
        if (request == null || !StringUtils.hasText(request.refreshToken())) {
            throw new BadRequestException("Refresh Token을 입력해주세요.");
        }
    }

    public void validateAuthenticatedUser(UserAuthResponse user) {
        if (user == null || user.userId() == null) {
            throw new UnauthorizedException("사용자 인증 정보를 확인할 수 없습니다.");
        }
    }
}
