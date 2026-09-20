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

    // 로그인 경로에서 User 모듈이 던지는 UserNotFoundException/DeletedUserException은
    // AuthService.authenticateWithUser()가 전부 붙잡아 401(이메일 또는 비밀번호가 일치하지
    // 않습니다)로 통일한다 — 계정 존재 여부가 상태 코드로 새어나가지 않게 하기 위해서다.
    // (그래서 이 두 예외의 핸들러는 여기 없다: auth 모듈 안에서 더는 밖으로 새어나올 경로가
    // 없다 — OAuth 로그인/가입 경로는 애초에 이 두 예외를 던지지 않는다.)

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
