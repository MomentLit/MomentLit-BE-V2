package com.example.user.controller;

import com.example.common.dto.ApiResponse;
import com.example.common.security.UserPrincipal;
import com.example.common.util.ResponseUtil;
import com.example.user.dto.request.SignUpRequest;
import com.example.user.dto.request.UserUpdateRequest;
import com.example.user.dto.response.SignUpResponse;
import com.example.user.dto.response.UserSearchResponse;
import com.example.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponse>> signup(
            @Valid @RequestBody SignUpRequest request
    ) {
        SignUpResponse response = userService.signup(request);

        ApiResponse<SignUpResponse> apiResponse = ResponseUtil.success("create user", response);

        return ResponseEntity.status(201).body(apiResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSearchResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserSearchResponse response = userService.getMyProfile(principal.userId());

        ApiResponse<UserSearchResponse> apiResponse = ResponseUtil.success("select my profile", response);

        return ResponseEntity.ok(apiResponse);
    }

    @PatchMapping("/me")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        userService.update(principal.userId(), request);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        userService.delete(principal.userId());

        return ResponseEntity.noContent().build();
    }
}
