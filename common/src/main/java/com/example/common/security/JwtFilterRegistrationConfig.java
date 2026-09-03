package com.example.common.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtFilterRegistrationConfig {

    // JwtAuthenticationFilter는 각 도메인의 SecurityFilterChain에 addFilterBefore로만 등록되어야 합니다.
    // @Component로도 등록돼 있어서, 그냥 두면 Spring Boot가 이 필터를 모든 요청(/*)에 걸리는
    // 전역 서블릿 필터로 한 번 더 자동 등록해버려 securityMatcher 스코프를 무시하고 실행됩니다.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
