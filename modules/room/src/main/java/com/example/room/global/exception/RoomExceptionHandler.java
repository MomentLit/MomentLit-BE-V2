package com.example.room.global.exception;

import com.example.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RoomExceptionHandler {

    @ExceptionHandler(InvalidRoomException.class)
    public ResponseEntity<ApiResponse<String>> handleInvalid(InvalidRoomException e) {
        log.warn("InvalidRoomException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Room/Invalid] " + e.getMessage()));
    }

    @ExceptionHandler({RoomNotFoundException.class, LayoutJobNotFoundException.class, FurnitureLayoutNotFoundException.class})
    public ResponseEntity<ApiResponse<String>> handleNotFound(RoomException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: Room/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(LayoutBusyException.class)
    public ResponseEntity<ApiResponse<String>> handleBusy(LayoutBusyException e) {
        log.warn("LayoutBusyException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("[ERROR: Room/Busy] " + e.getMessage()));
    }

    @ExceptionHandler(RoomException.class)
    public ResponseEntity<ApiResponse<String>> handleRoom(RoomException e) {
        log.error("RoomException", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Room/?] " + e.getMessage()));
    }
}
