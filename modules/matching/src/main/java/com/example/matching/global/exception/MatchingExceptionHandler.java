package com.example.matching.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MatchingExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> badRequestHandleException(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<String>> forbiddenHandleException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("[ERROR: Matching/Forbidden] " + e.getMessage()));
    }

    @ExceptionHandler(MatchingNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> matchingNotFoundHandleException(MatchingNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Matching/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(InvalidMatchingStateException.class)
    public ResponseEntity<ApiResponse<String>> invalidMatchingStateHandleException(InvalidMatchingStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: Matching/InvalidState] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.space.global.exception.SpaceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> spaceNotFoundHandleException(
            com.example.space.global.exception.SpaceNotFoundException e
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Space/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(MatchingException.class)
    public ResponseEntity<ApiResponse<String>> matchingHandleException(MatchingException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Matching/?] " + e.getMessage()));
    }
}
