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
                        .requestMatchers(HttpMethod.GET, "/spaces").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/spaces/*/schedule").permitAll()

                        .requestMatchers(HttpMethod.POST, "/spaces").authenticated()
                        .requestMatchers(HttpMethod.GET, "/spaces/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/spaces/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/spaces/*").authenticated()

                        .requestMatchers(HttpMethod.POST, "/spaces/*/likes").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/spaces/*/likes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/spaces/*/likes/me").authenticated()

                        .requestMatchers(HttpMethod.POST, "/spaces/*/schedule").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/spaces/*/schedule/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/spaces/*/schedule/*").authenticated()

                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
