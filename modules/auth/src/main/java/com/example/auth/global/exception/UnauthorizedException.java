package com.example.auth.global.exception;

public class UnauthorizedException extends AuthException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
