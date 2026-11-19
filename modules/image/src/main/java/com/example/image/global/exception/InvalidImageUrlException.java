package com.example.image.global.exception;

/** 우리 S3 버킷에 올라간 이미지 URL이 아님 — 서버가 임의 URL을 대신 내려받지 않도록(SSRF) 거른다. */
public class InvalidImageUrlException extends RuntimeException {
    public InvalidImageUrlException(String message) {
        super(message);
    }
}
