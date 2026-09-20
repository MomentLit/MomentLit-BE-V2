package com.example.suggestion.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SuggestionExceptionHandler {

    @ExceptionHandler(SuggestionNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNotFound(SuggestionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Suggestion/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(SuggestionForbiddenException.class)
    public ResponseEntity<ApiResponse<String>> handleForbidden(SuggestionForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("[ERROR: Suggestion/Forbidden] " + e.getMessage()));
    }
}
