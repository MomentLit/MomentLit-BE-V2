package com.example.chating.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> badRequestHandleException(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<String>> forbiddenHandleException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("[ERROR: Chat/Forbidden] " + e.getMessage()));
    }

    @ExceptionHandler(ChatRoomNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> chatRoomNotFoundHandleException(ChatRoomNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Chat/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.space.global.exception.SpaceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> spaceNotFoundHandleException(
            com.example.space.global.exception.SpaceNotFoundException e
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Space/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiResponse<String>> chatHandleException(ChatException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Chat/?] " + e.getMessage()));
    }
}
