package com.example.image.global.exception;

public class EmptyImageFileException extends RuntimeException {
    public EmptyImageFileException(String message) {
        super(message);
    }
}
