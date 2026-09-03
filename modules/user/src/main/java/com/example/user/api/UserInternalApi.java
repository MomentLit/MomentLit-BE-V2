package com.example.user.api;

import com.example.user.dto.request.SignInRequest;
import com.example.user.dto.request.UserOauthRequest;
import com.example.user.dto.response.UserAuthResponse;
import com.example.user.dto.response.UserNameResponse;

public interface UserInternalApi {

    UserAuthResponse authenticate(SignInRequest request);

    UserAuthResponse authenticateOauth(UserOauthRequest request);

    UserNameResponse getUserName(String userId);
}
