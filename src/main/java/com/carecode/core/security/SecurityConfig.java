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
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import com.carecode.core.security.CustomOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import com.carecode.core.security.CustomUserDetailsService;

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

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, CustomOAuth2UserService customOAuth2UserService, CustomUserDetailsService customUserDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customUserDetailsService = customUserDetailsService;
        log.info("SecurityConfig 초기화 - JWT 필터 등록됨");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("SecurityFilterChain 설정 시작");
        
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .defaultSuccessUrl("/admin/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/admin/logout")
                .logoutSuccessUrl("/admin/login?logout")
            )
            .userDetailsService(customUserDetailsService)
            .authorizeHttpRequests(authz -> authz
                // 공개 엔드포인트
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                // 인증 관련 엔드포인트
                .requestMatchers("/auth/login", "/auth/register").permitAll()
                .requestMatchers("/users/send-code", "/users/verify-code", "/users/verify").permitAll()
                .requestMatchers("/admin/login").permitAll()
                // 관리자 엔드포인트 (ADMIN만 접근)
                .requestMatchers("/admin/dashboard").hasRole("ADMIN")
                .requestMatchers("/admin/users/**").hasRole("ADMIN")
                .requestMatchers("/admin/hospitals/**").hasRole("ADMIN")
                .requestMatchers("/admin/policies/**").hasRole("ADMIN")
                .requestMatchers("/admin/community/**").hasRole("ADMIN")
                // API 엔드포인트 (인증 필요)
                .requestMatchers("/auth/**").authenticated()
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
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // H2 콘솔 사용 시 필요

        http.oauth2Login(oauth2Login -> oauth2Login
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService)
            )
        );

        log.info("SecurityFilterChain 설정 완료");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

} 