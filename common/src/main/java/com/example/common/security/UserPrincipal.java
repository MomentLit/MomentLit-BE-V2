package com.example.common.security;

public record UserPrincipal(
        String userId,
        Role role
) {
}
