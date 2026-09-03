package com.example.auth.global.exception;

public class BadRequestException extends AuthException {
    public BadRequestException(String message) {
        super(message);
    }
}
