package com.example.auth.global.exception;

public class TokenNotFoundException extends AuthException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
