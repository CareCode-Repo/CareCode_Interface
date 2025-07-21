package com.carecode.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        log.info("SecurityConfig 초기화 - JWT 필터 등록됨");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityFilterChain 설정 시작");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                
                // 인증 관련 엔드포인트
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/auth/**").authenticated()
                
                // API 엔드포인트 (인증 필요)
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/facilities/**").authenticated()
                .requestMatchers("/policies/**").authenticated()
                .requestMatchers("/community/**").authenticated()
                .requestMatchers("/chatbot/**").authenticated()
                .requestMatchers("/health/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                
                // 기타 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions().disable()); // H2 콘솔 사용 시 필요

        log.info("SecurityFilterChain 설정 완료");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

} 