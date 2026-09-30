package com.example.image.global.exception;

public class PrivacyBlurFailedException extends RuntimeException {
    public PrivacyBlurFailedException(String message) {
        super(message);
    }

    public PrivacyBlurFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
