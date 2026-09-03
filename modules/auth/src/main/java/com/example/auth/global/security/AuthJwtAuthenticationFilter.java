package com.example.auth.global.security;

import io.jsonwebtoken.JwtException;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthJwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthJwtProvider jwtProvider;
    private final TokenExtractor tokenExtractor;

    public AuthJwtAuthenticationFilter(AuthJwtProvider jwtProvider, TokenExtractor tokenExtractor) {
        this.jwtProvider = jwtProvider;
        this.tokenExtractor = tokenExtractor;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 요청에 Access Token이 있으면 검증 후 SecurityContext에 인증 정보를 저장
            tokenExtractor.extractAccessToken(request)
                    .ifPresent(this::setAuthentication);

            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            // 토큰이 위조됐거나 만료된 경우 인증 실패 응답을 내려줌
            SecurityContextHolder.clearContext();
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "유효하지 않은 JWT 토큰입니다.");
        }
    }

    private void setAuthentication(String token) {
        jwtProvider.validateToken(token);
        Authentication authentication = jwtProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
