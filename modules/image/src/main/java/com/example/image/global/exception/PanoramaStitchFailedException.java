package com.example.image.global.exception;

public class PanoramaStitchFailedException extends RuntimeException {
    public PanoramaStitchFailedException(String message) {
        super(message);
    }

    public PanoramaStitchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
