package com.example.alarm.global.exception;

import com.example.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AlarmExceptionHandler {

    // 이 핸들러는 alarm 모듈 소속이지만 @RestControllerAdvice는 앱 전체에 걸쳐 등록되므로,
    // 실제로는 모든 모듈의 @Valid 검증 실패(회원가입, 프로필 수정 등)가 전부 여기로 온다.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> methodArgumentNotValidHandleException(MethodArgumentNotValidException e) {
        // 필드명(예: "password")을 그대로 문장 앞에 붙이면 "password 비밀번호는 8자 이상..."처럼
        // 영문 필드명이 한글 문장 앞에 어색하게 끼어든다 — 검증 메시지 자체가 이미 완결된
        // 문장(예: "비밀번호는 8자 이상이어야 함")이라 필드명 없이 그대로 보여주면 충분하다.
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("잘못된 요청입니다.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + message));
    }
}
