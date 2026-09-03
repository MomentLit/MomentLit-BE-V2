package com.example.auth.service;

import com.example.auth.dto.request.RefreshRequest;
import com.example.auth.dto.request.SignInRequest;
import com.example.auth.dto.request.SignOutRequest;
import com.example.auth.dto.response.OauthGoogleCallbackResponse;
import com.example.auth.dto.response.RefreshResponse;
import com.example.auth.dto.response.SignInResponse;
import com.example.auth.global.client.GoogleOauthClient;
import com.example.auth.global.client.KakaoOauthClient;
import com.example.auth.global.client.NaverOauthClient;
import com.example.auth.global.client.dto.response.GoogleTokenResponse;
import com.example.auth.global.client.dto.response.GoogleUserInfoResponse;
import com.example.auth.global.client.dto.response.KakaoTokenResponse;
import com.example.auth.global.client.dto.response.KakaoUserInfoResponse;
import com.example.auth.global.client.dto.response.NaverTokenResponse;
import com.example.auth.global.client.dto.response.NaverUserInfoResponse;
import com.example.auth.global.client.dto.response.OauthUserProfile;
import com.example.auth.global.exception.GoogleOauthException;
import com.example.auth.global.exception.KakaoOauthException;
import com.example.auth.global.exception.NaverOauthException;
import com.example.auth.global.exception.UnauthorizedException;
import com.example.user.api.UserInternalApi;
import com.example.user.dto.response.UserAuthResponse;
import com.example.user.global.exception.InvalidPasswordException;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final GoogleOauthClient googleOauthClient;
    private final NaverOauthClient naverOauthClient;
    private final KakaoOauthClient kakaoOauthClient;
    private final UserInternalApi userApi;
    private final AuthValidator authValidator;
    private final RoleProcessor roleProcessor;
    private final TokenService tokenService;

    public AuthService(
            GoogleOauthClient googleOauthClient,
            NaverOauthClient naverOauthClient,
            KakaoOauthClient kakaoOauthClient,
            UserInternalApi userApi,
            AuthValidator authValidator,
            RoleProcessor roleProcessor,
            TokenService tokenService
    ) {
        this.googleOauthClient = googleOauthClient;
        this.naverOauthClient = naverOauthClient;
        this.kakaoOauthClient = kakaoOauthClient;
        this.userApi = userApi;
        this.authValidator = authValidator;
        this.roleProcessor = roleProcessor;
        this.tokenService = tokenService;
    }

    // 로그인
    public SignInResponse login(SignInRequest request) {

        authValidator.validateLoginRequest(request);

        UserAuthResponse user = authenticateWithUser(request);

        authValidator.validateAuthenticatedUser(user);

        String role =
                roleProcessor.normalizeRole(user.role());

        TokenService.TokenPair tokenPair =
                tokenService.issueTokens(
                        user.userId(),
                        role
                );

        return new SignInResponse(
                user.name(),
                role,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenService.accessTokenExpiresInSeconds()
        );
    }

    // User 모듈이 비밀번호 불일치를 InvalidPasswordException으로 알려주면,
    // 기존에 UserServiceClient가 401 응답을 번역하던 것과 같은 메시지로 맞춰줍니다.
    private UserAuthResponse authenticateWithUser(SignInRequest request) {
        try {
            return userApi.authenticate(
                    new com.example.user.dto.request.SignInRequest(request.email(), request.password())
            );
        } catch (InvalidPasswordException e) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // Google OAuth 로그인 페이지로 이동할 URL 생성
    public URI createGoogleAuthorizationUri(String state) {
        return googleOauthClient.createAuthorizationUri(state);
    }

    // Google OAuth callback code로 사용자 인증 후 JWT 발급
    public OauthGoogleCallbackResponse loginWithGoogle(String code, String state) {

        authValidator.validateGoogleAuthorizationCode(code);

        GoogleTokenResponse googleToken =
                googleOauthClient.requestToken(code);

        if (googleToken == null || !StringUtils.hasText(googleToken.accessToken())) {
            throw new GoogleOauthException("Google Access Token을 발급받을 수 없습니다.");
        }

        GoogleUserInfoResponse googleUser =
                googleOauthClient.requestUserInfo(googleToken.accessToken());

        return issueOauthLoginResponse(googleUser.toProfile(), "Google");
    }

    public URI createNaverAuthorizationUri(String state) {
        return naverOauthClient.createAuthorizationUri(state);
    }

    public OauthGoogleCallbackResponse loginWithNaver(String code, String state) {

        authValidator.validateOauthAuthorizationCode(code, "Naver");

        NaverTokenResponse naverToken =
                naverOauthClient.requestToken(code, state);

        if (naverToken == null || !StringUtils.hasText(naverToken.accessToken())) {
            throw new NaverOauthException("Naver Access Token을 발급받을 수 없습니다.");
        }

        NaverUserInfoResponse naverUser =
                naverOauthClient.requestUserInfo(naverToken.accessToken());

        return issueOauthLoginResponse(naverUser.toProfile(), "Naver");
    }

    public URI createKakaoAuthorizationUri(String state) {
        return kakaoOauthClient.createAuthorizationUri(state);
    }

    public OauthGoogleCallbackResponse loginWithKakao(String code, String state) {

        authValidator.validateOauthAuthorizationCode(code, "Kakao");

        KakaoTokenResponse kakaoToken =
                kakaoOauthClient.requestToken(code);

        if (kakaoToken == null || !StringUtils.hasText(kakaoToken.accessToken())) {
            throw new KakaoOauthException("Kakao Access Token을 발급받을 수 없습니다.");
        }

        KakaoUserInfoResponse kakaoUser =
                kakaoOauthClient.requestUserInfo(kakaoToken.accessToken());

        return issueOauthLoginResponse(kakaoUser.toProfile(), "Kakao");
    }

    private OauthGoogleCallbackResponse issueOauthLoginResponse(
            OauthUserProfile profile,
            String providerName
    ) {
        authValidator.validateOauthProfile(profile, providerName);

        UserAuthResponse user = userApi.authenticateOauth(
                new com.example.user.dto.request.UserOauthRequest(
                        profile.provider(),
                        profile.providerId(),
                        profile.email(),
                        profile.emailVerified(),
                        profile.name(),
                        profile.imageUrl()
                )
        );

        authValidator.validateAuthenticatedUser(user);

        String role =
                roleProcessor.normalizeRole(user.role());

        TokenService.TokenPair tokenPair =
                tokenService.issueTokens(
                        user.userId(),
                        role
                );

        return new OauthGoogleCallbackResponse(
                user.name(),
                role,
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenService.accessTokenExpiresInSeconds()
        );
    }

    // 토큰 재발급
    public RefreshResponse refresh(RefreshRequest request) {

        authValidator.validateRefreshRequest(request);

        String subject =
                tokenService.getValidRefreshTokenSubject(
                        request.refreshToken()
                );

        String role =
                tokenService.getRole(request.refreshToken());

        TokenService.TokenPair tokenPair =
                tokenService.issueTokens(subject, role);

        return new RefreshResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenService.accessTokenExpiresInSeconds()
        );
    }

    // 로그아웃
    public void logout(SignOutRequest request) {

        authValidator.validateSignOutRequest(request);

        tokenService.deleteRefreshToken(request.refreshToken());
    }
}
