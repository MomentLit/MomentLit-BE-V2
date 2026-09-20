package com.example.space.global.config;

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
public class SpaceSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    @Order(3)
    public SecurityFilterChain spaceSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                .securityMatcher("/spaces/**", "/internal/spaces/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Spring Security는 먼저 매치되는 규칙을 그대로 쓴다(첫 매치 승리) — "/spaces/*"는
                        // "/spaces/me"에도 매치되므로, 이 permitAll 와일드카드보다 "/spaces/me" 같은 더
                        // 구체적인 authenticated() 규칙을 반드시 먼저 선언해야 한다. 순서가 바뀌면
                        // 토큰이 없거나 만료된 요청도 "/spaces/me"를 인증 없이 통과해버리고, 컨트롤러가
                        // null principal로 NPE(500)를 던지게 된다 — 실제로 겪은 버그.
                        .requestMatchers(HttpMethod.GET, "/spaces/me").authenticated()

                        .requestMatchers(HttpMethod.GET, "/spaces").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/counts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/*/schedule").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/*/availability").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/*/booked-dates").permitAll()

                        .requestMatchers(HttpMethod.POST, "/spaces").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/spaces/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/spaces/*").authenticated()

                        .requestMatchers(HttpMethod.POST, "/spaces/*/likes").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/spaces/*/likes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/spaces/*/likes/me").authenticated()

                        .requestMatchers(HttpMethod.POST, "/spaces/*/schedule").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/spaces/*/schedule/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/spaces/*/schedule/*").authenticated()

                        .requestMatchers(HttpMethod.PUT, "/spaces/*/availability").authenticated()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
