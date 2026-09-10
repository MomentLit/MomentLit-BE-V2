package com.example.popup.global.config;

import com.example.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class PopupSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    @Order(6)
    public SecurityFilterChain popupSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/popups/**", "/popup-reviews/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/popups"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/popups/me"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/popups/*/likes"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/popups/*/likes"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/popups/*/likes/me"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/popups/*/reviews",
                                "/popup-reviews/**"
                        ).authenticated()
                        .requestMatchers(
                                "/popup-reviews/**"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
