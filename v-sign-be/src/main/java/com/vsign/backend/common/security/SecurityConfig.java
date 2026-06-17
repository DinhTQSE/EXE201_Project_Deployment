package com.vsign.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsign.backend.common.exception.ApiErrorResponse;
import com.vsign.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String allowedOrigins
    ) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, ErrorCode.FORBIDDEN)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(
                                "/api/v1/swagger-ui/**",
                                "/api/v1/api-docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/version").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/dictionary/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/learning/practice-items/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/units/**", "/api/v1/chapters/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/lessons/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/subscription/plans", "/api/v1/subscriptions/plans").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/assessments/**").permitAll()
                        .requestMatchers("/api/v1/lessons/*/quiz", "/api/v1/lessons/*/progress", "/api/v1/lessons/*/complete").authenticated()
                        .requestMatchers("/api/v1/quiz-attempts/**", "/api/v1/signature-workflows/**").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/payments/tiers").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/payments/payos/webhook").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/payos/webhook").permitAll()
                        .requestMatchers("/api/v1/payments/**", "/api/v1/subscriptions/checkout").authenticated()
                        .requestMatchers("/api/v1/me", "/api/v1/me/**", "/api/v1/gamification/**", "/api/v1/leaderboards").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "CONTENT_REVIEWER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseOrigins(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws java.io.IOException {
        response.setStatus(errorCode.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.of(errorCode, errorCode.defaultMessage()));
    }

    private List<String> parseOrigins(String origins) {
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
