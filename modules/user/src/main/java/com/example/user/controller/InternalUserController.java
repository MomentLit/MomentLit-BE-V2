package com.example.user.controller;

import com.example.user.dto.request.SignInRequest;
import com.example.user.dto.request.UserOauthRequest;
import com.example.user.dto.response.UserAuthResponse;
import com.example.user.dto.response.UserNameResponse;
import com.example.user.service.InternalUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/users")
public class InternalUserController {

    private final InternalUserService internalUserService;

    @PostMapping("/authenticate")
    public ResponseEntity<UserAuthResponse> authenticate(
            @RequestBody SignInRequest request
    ) {
        UserAuthResponse response = internalUserService.authenticate(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth")
    public ResponseEntity<UserAuthResponse> authenticateOauth(
            @RequestBody UserOauthRequest request
    ) {
        return ResponseEntity.ok(internalUserService.authenticateOauth(request));
    }

    @GetMapping("/{user-id}/name")
    public ResponseEntity<UserNameResponse> getUserName(
            @PathVariable("user-id") String userId
    ) {
        return ResponseEntity.ok(internalUserService.getUserName(userId));
    }
}
