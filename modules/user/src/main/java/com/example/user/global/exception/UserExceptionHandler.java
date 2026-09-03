package com.example.user.global.exception;

import com.example.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> badRequestHandleException(BadRequestException e) {
        log.warn("BadRequestException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + e.getMessage()));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<String>> invalidPasswordHandleException(InvalidPasswordException e) {
        log.warn("InvalidPasswordException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("[ERROR: User/Auth/InvalidPassword] " + e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> userNotFoundHandleException(UserNotFoundException e) {
        log.warn("UserNotFoundException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: User/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<String>> duplicateEmailHandleException(DuplicateEmailException e) {
        log.warn("DuplicateEmailException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: User/Email/Duplicate] " + e.getMessage()));
    }

    @ExceptionHandler(DeletedUserException.class)
    public ResponseEntity<ApiResponse<String>> deletedUserHandleException(DeletedUserException e) {
        log.warn("DeletedUserException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.fail("[ERROR: User/Deleted] " + e.getMessage()));
    }

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<String>> userHandleException(UserException e) {
        log.error("UserException", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: User/?] " + e.getMessage()));
    }
}
