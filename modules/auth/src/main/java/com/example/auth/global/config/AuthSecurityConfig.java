package com.example.auth.global.config;

import com.example.auth.global.security.AuthJwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class AuthSecurityConfig {

    private final AuthJwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public AuthSecurityConfig(
            AuthJwtAuthenticationFilter jwtAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    @Order(4)
    public SecurityFilterChain authSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/auth/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // JWT 인증은 세션을 사용하지 않으므로 CSRF와 세션 생성 X
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 로그인과 토큰 재발급 API는 인증 없이 접근할 수 있게 열어둠
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/signin",
                                "/auth/refresh",
                                "/auth/oauth/google",
                                "/auth/oauth/google/callback",
                                "/auth/oauth/naver",
                                "/auth/oauth/naver/callback",
                                "/auth/oauth/kakao",
                                "/auth/oauth/kakao/callback",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // UsernamePasswordAuthenticationFilter 전에 JWT 필터를 실행해 요청 인증을 먼저 처리
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
