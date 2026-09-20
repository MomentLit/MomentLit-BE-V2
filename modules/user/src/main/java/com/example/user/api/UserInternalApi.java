package com.example.user.api;

import com.example.user.dto.request.SignInRequest;
import com.example.user.dto.request.UserOauthRequest;
import com.example.user.dto.response.UserAuthResponse;
import com.example.user.dto.response.UserNameResponse;
import com.example.user.dto.response.UserProfileResponse;

public interface UserInternalApi {

    UserAuthResponse authenticate(SignInRequest request);

    UserAuthResponse authenticateOauth(UserOauthRequest request);

    UserNameResponse getUserName(String userId);

    /** 이름+프로필 사진 정도만 필요한 소비자용 — 예: 공간 상세의 호스트 표시. */
    UserProfileResponse getUserProfile(String userId);
}
