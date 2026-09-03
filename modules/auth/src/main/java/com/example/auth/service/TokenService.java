package com.example.auth.service;

import com.example.auth.global.security.AuthJwtProvider;
import com.example.auth.global.exception.TokenNotFoundException;
import com.example.auth.global.exception.UnauthorizedException;
import com.example.auth.infra.RefreshTokenRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private final AuthJwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public TokenService(
            AuthJwtProvider jwtProvider,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public TokenPair issueTokens(
            String subject,
            String role
    ) {

        String accessToken =
                jwtProvider.createAccessToken(subject, role);

        String refreshToken =
                jwtProvider.createRefreshToken(subject, role);

        refreshTokenRepository.save(
                subject,
                refreshToken,
                jwtProvider.getRefreshTokenExpirationMillis()
        );

        return new TokenPair(
                accessToken,
                refreshToken
        );
    }

    public long accessTokenExpiresInSeconds() {
        return jwtProvider.getAccessTokenExpirationMillis() / 1000;
    }

    public String getValidRefreshTokenSubject(String refreshToken) {
        try {
            jwtProvider.validateToken(refreshToken);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }

        String subject = jwtProvider.getSubject(refreshToken);

        if (!refreshTokenRepository.existsBySubjectAndToken(subject, refreshToken)) {
            throw new TokenNotFoundException(
                    "저장된 Refresh Token과 일치하지 않습니다."
            );
        }

        return subject;
    }

    public String getRole(String token) {
        return jwtProvider.getRole(token);
    }

    public void deleteRefreshToken(String refreshToken) {
        String subject =
                getValidRefreshTokenSubject(refreshToken);

        refreshTokenRepository.deleteBySubject(subject);
    }

    public record TokenPair(
            String accessToken,
            String refreshToken
    ) {}
}
