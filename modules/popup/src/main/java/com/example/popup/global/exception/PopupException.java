package com.example.popup.global.exception;

public class PopupException extends RuntimeException {
    public PopupException(String message) {
        super(message);
    }

    public PopupException(String message, Throwable cause) {
        super(message, cause);
    }
}
