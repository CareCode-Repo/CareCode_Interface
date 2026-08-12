package com.carecode.core.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

/** Spring Security 설정 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final List<String> allowedOrigins;
    private final Environment environment;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, 
                         CustomUserDetailsService customUserDetailsService,
                         Environment environment,
                         @Value("${app.security.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000}") String allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customUserDetailsService = customUserDetailsService;
        this.environment = environment;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /** 전체 API 체인. 어드민도 동일한 JWT 인증을 사용하고 /api/admin/** 에서 ADMIN 역할로 구분한다 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowCredentials(true);
                configuration.setAllowedOriginPatterns(allowedOrigins);
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setExposedHeaders(List.of("Authorization", "X-Refresh-Token"));
                return configuration;
            })) // CORS 활성화
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
            .authorizeHttpRequests(authz -> {
                if (!environment.matchesProfiles("prod")) {
                    authz.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll();
                }
                if (environment.matchesProfiles("dev", "docker")) {
                    authz.requestMatchers("/kakao-test.html", "/kakao-debug.html").permitAll();
                }
                if (!environment.matchesProfiles("prod")) {
                    authz.requestMatchers("/*.html").permitAll();
                }
                authz
                // 공개 엔드포인트
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                // 약관·방침은 동의하기 전에 읽어야 하므로 비로그인도 볼 수 있어야 한다.
                .requestMatchers("/legal/**").permitAll()
                
                // 정적 리소스 (공개 접근)
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/static/**").permitAll()
                
                // 통합 인증 관련 엔드포인트 (공개 접근)
                .requestMatchers("/auth/login", "/auth/register").permitAll() // 일반 로그인/회원가입
                .requestMatchers("/auth/refresh").permitAll() // 토큰 갱신
                .requestMatchers("/auth/kakao/login").permitAll() // 카카오 로그인
                .requestMatchers("/auth/kakao/login-url").permitAll() // 카카오 로그인 URL 생성
                .requestMatchers("/auth/kakao/complete-registration").permitAll() // 카카오 가입 완료
                
                // OAuth2 authorize/token (Spring Client beans).
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/kakao-callback.html").permitAll()
                
                // 이메일 인증 관련 엔드포인트 (공개 접근)
                .requestMatchers("/users/send-code", "/users/verify-code", "/users/verify").permitAll()
                
                // 관리자 API (ADMIN 권한 필요)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 공개 API 엔드포인트
                // 대기 기록은 본인 것만 다루므로 인증이 필요하다. 와일드카드보다 먼저 선언한다.
                .requestMatchers("/facilities/waitlist/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/facilities/*/waitlist").authenticated()
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
                .requestMatchers(HttpMethod.POST, "/facilities/*/rating").authenticated()
                .requestMatchers("/facilities/*/rating").permitAll()
                
                // 돌봄시설 공공데이터 API (공개 접근)
                .requestMatchers("/api/public/care-facilities/**").permitAll()
                
                // 병원 조회는 로그인 전에도 보여야 한다. 실제 경로가 /health/hospitals/** 라
                // 아래 /health/** 규칙보다 먼저 선언해야 한다.
                .requestMatchers(HttpMethod.GET, "/health/hospitals/statistics").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/nearby").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/popular").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/type/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/*/reviews").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/*/likes").permitAll()
                // 좋아요 여부는 "내" 상태라 로그인이 필요하다.
                .requestMatchers("/health/hospitals/*/like-status").authenticated()

                // 건강 API: 인증된 사용자만 (소유권은 서비스 계층에서 검증)
                .requestMatchers("/health/**").authenticated()
                
                // 정책 API: 개인화·북마크는 인증 필요, 나머지 조회는 공개 아래 /policies/* 와일드카드보다 먼저 선언해야 적용된다.
                .requestMatchers("/policies/recommendations").authenticated()
                .requestMatchers("/policies/missed-benefits").authenticated()
                .requestMatchers("/policies/regional-comparison").authenticated()
                .requestMatchers(HttpMethod.POST, "/policies/*/amount-reports").authenticated()
                .requestMatchers("/policies/bookmarks").authenticated()
                .requestMatchers("/policies/*/bookmarks").authenticated()
                .requestMatchers("/policies").permitAll()
                .requestMatchers("/policies/search").permitAll()
                .requestMatchers("/policies/categories").permitAll()
                .requestMatchers("/policies/*").permitAll()
                .requestMatchers("/policies/statistics").permitAll()
                
                // CORS preflight 요청 허용
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 커뮤니티 API - 조회는 공개, 작성/수정/삭제는 인증 필요
                .requestMatchers(HttpMethod.GET, "/community/posts").permitAll() // 게시글 목록 조회
                .requestMatchers(HttpMethod.GET, "/community/posts/*").permitAll() // 게시글 상세 조회
                .requestMatchers(HttpMethod.GET, "/community/search").permitAll() // 게시글 검색
                .requestMatchers(HttpMethod.GET, "/community/popular").permitAll() // 인기 게시글
                .requestMatchers(HttpMethod.GET, "/community/latest").permitAll() // 최신 게시글
                .requestMatchers(HttpMethod.GET, "/community/posts/*/comments").permitAll() // 댓글 조회
                .requestMatchers(HttpMethod.GET, "/community/tags").permitAll() // 태그 목록
                .requestMatchers(HttpMethod.GET, "/community/tags/**").permitAll() // 태그 관련 조회
                .requestMatchers(HttpMethod.GET, "/community/search/all").permitAll() // 전체 검색
                .requestMatchers(HttpMethod.GET, "/community/popular/limit").permitAll() // 제한된 인기 게시글
                .requestMatchers(HttpMethod.GET, "/community/latest/limit").permitAll() // 제한된 최신 게시글
                
                // 인증이 필요한 API 엔드포인트 (로그인/회원가입 제외)
                .requestMatchers("/auth/user/**").authenticated()
                .requestMatchers("/auth/logout").authenticated()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/facilities/search").authenticated()
                .requestMatchers("/facilities/*/bookings/**").authenticated()
                .requestMatchers("/community/comments/**").authenticated()
                .requestMatchers("/notifications/**").authenticated()
                
                // 기타 모든 요청은 인증 필요
                .anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // H2 콘솔 사용 시 필요
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** @Component 로 선언된 필터는 Boot 가 서블릿 컨테이너에 자동 등록한다. */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
} 