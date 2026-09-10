package com.example.review.global.exception;

public class BadRequestException extends ReviewException {

    public BadRequestException(String message) {
        super(message);
    }
}
