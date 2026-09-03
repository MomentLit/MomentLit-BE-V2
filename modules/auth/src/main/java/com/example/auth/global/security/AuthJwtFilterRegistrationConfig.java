package com.example.auth.global.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthJwtFilterRegistrationConfig {

    // AuthSecurityConfig의 addFilterBefore로만 등록돼야 하는데, @Component 때문에
    // Spring Boot가 이 필터를 모든 요청(/*)에 걸리는 전역 필터로 한 번 더 자동 등록해버립니다.
    @Bean
    public FilterRegistrationBean<AuthJwtAuthenticationFilter> authJwtAuthenticationFilterRegistration(
            AuthJwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<AuthJwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
