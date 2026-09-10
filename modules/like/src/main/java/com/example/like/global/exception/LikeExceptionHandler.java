package com.example.like.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LikeExceptionHandler {

    @ExceptionHandler(DuplicatePopupLikeException.class)
    public ResponseEntity<ApiResponse<String>> handleConflict(DuplicatePopupLikeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: Like/Conflict] " + e.getMessage()));
    }

    @ExceptionHandler(PopupLikeNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(PopupLikeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Like/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler({DuplicateSpaceLikeException.class, DuplicateReviewLikeException.class})
    public ResponseEntity<ApiResponse<String>> handleDuplicateLike(LikeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: Like/Conflict] " + e.getMessage()));
    }

    @ExceptionHandler({SpaceLikeNotFoundException.class, ReviewLikeNotFoundException.class})
    public ResponseEntity<ApiResponse<String>> handleLikeNotFound(LikeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Like/NotFound] " + e.getMessage()));
    }
}
