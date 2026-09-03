package com.example.auth.dto.request;

public record SignInRequest(
        String email,
        String password
) {
}
