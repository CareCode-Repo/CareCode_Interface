package com.carecode.core.security;

import jakarta.servlet.http.HttpServletResponse;
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
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomUserDetailsService customUserDetailsService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                         CustomOAuth2UserService customOAuth2UserService, 
                         CustomUserDetailsService customUserDetailsService,
                         OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                         OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customUserDetailsService = customUserDetailsService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        log.info("SecurityConfig 초기화 - JWT 필터 및 OAuth2 핸들러 등록됨");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityFilterChain 설정 시작");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .userDetailsService(customUserDetailsService)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Access denied\"}");
                })
            )
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                
                // 정적 리소스 (공개 접근)
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/static/**", "/*.html").permitAll()
                
                // 인증 관련 엔드포인트 (공개 접근)
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/users/send-code", "/users/verify-code", "/users/verify").permitAll()
                
                // OAuth2 소셜 로그인 (공개 접근)
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/login/oauth2/**").permitAll()
                
                // 관리자 API (ADMIN 권한 필요)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 공개 API 엔드포인트
                .requestMatchers("/facilities").permitAll()
                .requestMatchers("/facilities/type/**").permitAll()
                .requestMatchers("/facilities/location/**").permitAll()
                .requestMatchers("/facilities/age").permitAll()
                .requestMatchers("/facilities/operating-hours").permitAll()
                .requestMatchers("/facilities/popular").permitAll()
                .requestMatchers("/facilities/new").permitAll()
                .requestMatchers("/facilities/radius").permitAll()
                .requestMatchers("/facilities/statistics").permitAll()
                .requestMatchers("/facilities/*/view").permitAll()
                .requestMatchers("/facilities/*/rating").permitAll()
                
                // Health API 엔드포인트 (공개 접근)
                .requestMatchers("/health/records").permitAll()
                .requestMatchers("/health/statistics").permitAll()
                .requestMatchers("/health/goals").permitAll()
                .requestMatchers("/health/records/*").permitAll()
                .requestMatchers("/health/statistics/*").permitAll()
                .requestMatchers("/health/goals/*").permitAll()
                .requestMatchers("/health/records/user/*").permitAll()
                .requestMatchers("/health/records/user/*/chart").permitAll()
                
                // Community API 엔드포인트 (공개 접근)
                .requestMatchers("/community/posts").permitAll()
                .requestMatchers("/community/tags").permitAll()
                .requestMatchers("/community/posts/*").permitAll()
                .requestMatchers("/community/posts/*/comments").permitAll()
                .requestMatchers("/community/search").permitAll()
                .requestMatchers("/community/popular").permitAll()
                .requestMatchers("/community/latest").permitAll()
                
                // Policy API 엔드포인트 (공개 접근)
                .requestMatchers("/policies").permitAll()
                .requestMatchers("/policies/*").permitAll()
                .requestMatchers("/policies/categories").permitAll()
                .requestMatchers("/policies/category/**").permitAll()
                .requestMatchers("/policies/location/**").permitAll()
                .requestMatchers("/policies/age").permitAll()
                .requestMatchers("/policies/popular").permitAll()
                .requestMatchers("/policies/latest").permitAll()
                .requestMatchers("/policies/statistics").permitAll()
                
                // 인증이 필요한 API 엔드포인트 (로그인/회원가입 제외)
                .requestMatchers("/auth/user/**").authenticated()
                .requestMatchers("/auth/logout").authenticated()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/facilities/search").authenticated()
                .requestMatchers("/facilities/*/bookings/**").authenticated()
                .requestMatchers("/community/comments/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                
                // 챗봇 API (공개 접근 허용)
                .requestMatchers("/chatbot/chat").permitAll()
                .requestMatchers("/chatbot/history").permitAll()
                .requestMatchers("/chatbot/sessions").permitAll()
                .requestMatchers("/chatbot/feedback").permitAll()
                
                // 기타 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // H2 콘솔 사용 시 필요

        http.oauth2Login(oauth2Login -> oauth2Login
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
            .successHandler(oAuth2AuthenticationSuccessHandler)
            .failureHandler(oAuth2AuthenticationFailureHandler)
        );

        log.info("SecurityFilterChain 설정 완료");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



} 