package com.example.user.dto.request;

public record SignInRequest(
        String email,
        String password
) {
}
