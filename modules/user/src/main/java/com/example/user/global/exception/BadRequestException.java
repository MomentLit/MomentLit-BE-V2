package com.example.user.global.exception;

public class BadRequestException extends UserException {
    public BadRequestException(String message) {
        super(message);
    }
}
