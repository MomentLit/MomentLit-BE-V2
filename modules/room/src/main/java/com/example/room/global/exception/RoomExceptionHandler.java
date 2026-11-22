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

    @ExceptionHandler({RoomNotFoundException.class, LayoutJobNotFoundException.class, FurnitureLayoutNotFoundException.class, CustomFurnitureNotFoundException.class})
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

    /** 추론·도면 인식 서비스 장애 — 자세한 원인은 로그에만 남기고, 사용자에게는 다시 시도하거나 직접 그리라고만 알린다. */
    @ExceptionHandler(LayoutUnavailableException.class)
    public ResponseEntity<ApiResponse<String>> handleUnavailable(LayoutUnavailableException e) {
        log.error("LayoutUnavailableException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail("[ERROR: Room/Unavailable] 자동 인식을 지금 쓸 수 없어요. 잠시 후 다시 시도하거나 직접 그려 주세요."));
    }

    @ExceptionHandler(RoomException.class)
    public ResponseEntity<ApiResponse<String>> handleRoom(RoomException e) {
        log.error("RoomException", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Room/?] " + e.getMessage()));
    }
}
