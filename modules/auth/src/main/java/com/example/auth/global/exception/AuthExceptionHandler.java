package com.example.auth.global.exception;

import com.example.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> tokenNotFoundHandleException(TokenNotFoundException e) {
        log.warn("TokenNotFoundException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("[ERROR: Auth/Token/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> unauthorizedHandleException(UnauthorizedException e) {
        log.warn("UnauthorizedException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail("[ERROR: Auth/Unauthorized] " + e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> badRequestHandleException(BadRequestException e) {
        log.warn("BadRequestException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + e.getMessage()));
    }

    @ExceptionHandler(GoogleOauthException.class)
    public ResponseEntity<ApiResponse<String>> googleOauthHandleException(GoogleOauthException e) {
        log.error("GoogleOauthException", e);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("[ERROR: Auth/Oauth/Google] " + e.getMessage()));
    }

    @ExceptionHandler(NaverOauthException.class)
    public ResponseEntity<ApiResponse<String>> naverOauthHandleException(NaverOauthException e) {
        log.error("NaverOauthException", e);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("[ERROR: Auth/Oauth/Naver] " + e.getMessage()));
    }

    @ExceptionHandler(KakaoOauthException.class)
    public ResponseEntity<ApiResponse<String>> kakaoOauthHandleException(KakaoOauthException e) {
        log.error("KakaoOauthException", e);

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail("[ERROR: Auth/Oauth/Kakao] " + e.getMessage()));
    }

    // User 모듈을 인터페이스로 직접 호출하면서, 예전에 UserServiceClient가 HTTP 응답 바디에서
    // 그대로 forward하던 User 쪽 예외 메시지를 동일한 포맷으로 재현합니다.
    @ExceptionHandler(com.example.user.global.exception.UserNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> userNotFoundHandleException(
            com.example.user.global.exception.UserNotFoundException e
    ) {
        log.warn("UserNotFoundException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("[ERROR: User/NotFound] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.user.global.exception.DeletedUserException.class)
    public ResponseEntity<ApiResponse<String>> deletedUserHandleException(
            com.example.user.global.exception.DeletedUserException e
    ) {
        log.warn("DeletedUserException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiResponse.fail("[ERROR: User/Deleted] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.user.global.exception.DuplicateEmailException.class)
    public ResponseEntity<ApiResponse<String>> duplicateEmailHandleException(
            com.example.user.global.exception.DuplicateEmailException e
    ) {
        log.warn("DuplicateEmailException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail("[ERROR: User/Email/Duplicate] " + e.getMessage()));
    }

    @ExceptionHandler(com.example.user.global.exception.BadRequestException.class)
    public ResponseEntity<ApiResponse<String>> userBadRequestHandleException(
            com.example.user.global.exception.BadRequestException e
    ) {
        log.warn("User BadRequestException: {}", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("[ERROR: Request/BadRequest] " + e.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<String>> authHandleException(AuthException e) {
        log.error("AuthException", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("[ERROR: Auth/?] " + e.getMessage()));
    }
}
