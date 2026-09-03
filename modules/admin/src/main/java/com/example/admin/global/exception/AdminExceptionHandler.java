package com.example.admin.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AdminExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<String>> forbiddenHandleException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("[ERROR: Admin/Forbidden] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.space.global.exception.SpaceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> spaceNotFoundHandleException(
            com.example.space.global.exception.SpaceNotFoundException e
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Space/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ApiResponse<String>> preconditionFailedHandleException(PreconditionFailedException e) {
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                .body(ApiResponse.fail("[ERROR: Admin/PreconditionFailed] " + e.getMessage()));
    }

    @ExceptionHandler(AdminException.class)
    public ResponseEntity<ApiResponse<String>> adminHandleException(AdminException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Admin/?] " + e.getMessage()));
    }
}
