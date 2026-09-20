package com.example.common.exception;

import com.example.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // 애플리케이션 레벨 중복 체크를 통과했지만 동시 요청 등으로 DB unique 제약을 위반한 경우를 위한 안전망
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolationException", e);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: ?/Conflict] 이미 존재하는 값이거나 처리할 수 없는 요청입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(Exception e) {
        log.error("Unhandled exception", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: ?/?] 서버 내부 오류가 발생했습니다."));
    }
}
