package com.example.auth.controller;

import com.example.auth.dto.request.SignInRequest;
import com.example.auth.dto.request.RefreshRequest;
import com.example.auth.dto.request.SignOutRequest;
import com.example.auth.dto.response.OauthGoogleCallbackResponse;
import com.example.auth.dto.response.RefreshResponse;
import com.example.auth.dto.response.SignInResponse;
import com.example.auth.service.AuthService;
import com.example.common.dto.ApiResponse;
import com.example.common.util.ResponseUtil;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<SignInResponse>> signin(@RequestBody SignInRequest request) {
        // 로그인 성공 시 Access Token과 Refresh Token을 클라이언트에게 내려줍니다.
        SignInResponse response = authService.login(request);
        return ResponseEntity.ok(ResponseUtil.success("로그인에 성공했습니다.", response));
    }

    @GetMapping("/oauth/google")
    public ResponseEntity<Void> googleOauth(@RequestParam(required = false) String state) {
        // Google 로그인 페이지로 302 Redirect합니다.
        URI redirectUri = authService.createGoogleAuthorizationUri(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/oauth/google/callback")
    public ResponseEntity<ApiResponse<OauthGoogleCallbackResponse>> googleOauthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        // Google이 내려준 code로 사용자 정보를 확인한 뒤 JWT를 발급합니다.
        OauthGoogleCallbackResponse response =
                authService.loginWithGoogle(code, state);
        return ResponseEntity.ok(ResponseUtil.success("Google 로그인에 성공했습니다.", response));
    }

    @GetMapping("/oauth/naver")
    public ResponseEntity<Void> naverOauth(@RequestParam(required = false) String state) {
        URI redirectUri = authService.createNaverAuthorizationUri(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/oauth/naver/callback")
    public ResponseEntity<ApiResponse<OauthGoogleCallbackResponse>> naverOauthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        OauthGoogleCallbackResponse response =
                authService.loginWithNaver(code, state);
        return ResponseEntity.ok(ResponseUtil.success("Naver 로그인에 성공했습니다.", response));
    }

    @GetMapping("/oauth/kakao")
    public ResponseEntity<Void> kakaoOauth(@RequestParam(required = false) String state) {
        URI redirectUri = authService.createKakaoAuthorizationUri(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<ApiResponse<OauthGoogleCallbackResponse>> kakaoOauthCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state
    ) {
        OauthGoogleCallbackResponse response =
                authService.loginWithKakao(code, state);
        return ResponseEntity.ok(ResponseUtil.success("Kakao 로그인에 성공했습니다.", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(@RequestBody RefreshRequest request) {
        // Refresh Token이 유효하면 새로운 토큰 묶음을 발급합니다.
        RefreshResponse response = authService.refresh(request);
        return ResponseEntity.ok(ResponseUtil.success("토큰 재발급에 성공했습니다.", response));
    }

    @DeleteMapping("/signout")
    public ResponseEntity<ApiResponse<Void>> signout(@RequestBody SignOutRequest request) {
        // 로그아웃은 클라이언트가 가진 Refresh Token을 저장소에서 제거하는 방식입니다.
        authService.logout(request);
        return ResponseEntity.ok(ResponseUtil.success("로그아웃에 성공했습니다."));
    }
}
