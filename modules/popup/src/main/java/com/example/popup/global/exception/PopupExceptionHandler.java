package com.example.popup.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PopupExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> badRequestHandleException(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<String>> forbiddenHandleException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("[ERROR: Popup/Forbidden] " + e.getMessage()));
    }

    @ExceptionHandler(PopupNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> popupNotFoundHandleException(PopupNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Popup/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(InvalidPopupStateException.class)
    public ResponseEntity<ApiResponse<String>> invalidPopupStateHandleException(InvalidPopupStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: Popup/InvalidState] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.matching.global.exception.MatchingNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> matchingNotFoundHandleException(
            com.example.matching.global.exception.MatchingNotFoundException e
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Matching/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.space.global.exception.SpaceNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> spaceNotFoundHandleException(
            com.example.space.global.exception.SpaceNotFoundException e
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Space/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(PopupException.class)
    public ResponseEntity<ApiResponse<String>> popupHandleException(PopupException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Popup/?] " + e.getMessage()));
    }
}
