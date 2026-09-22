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
                // 프로필 이미지는 <img src> 로 불러가므로 인증 헤더를 붙일 수 없다.
                // 파일명이 UUID 라 주소를 모르면 찾을 수 없고, 원래 화면에 노출되는 값이다.
                //
                // 업로드 루트(/files/**) 전체를 열지 않는 이유는 같은 저장소에 건강기록 첨부가
                // 들어 있기 때문이다. 그쪽은 민감정보라 주소만 알면 열리는 상태로 두면 안 되고,
                // 인증을 거치는 별도 다운로드 경로가 필요하다.
                .requestMatchers("/files/profile-images/**").permitAll()
                .requestMatchers("/static/**").permitAll()
                
                // 통합 인증 관련 엔드포인트 (공개 접근)
                .requestMatchers("/auth/login", "/auth/register").permitAll() // 일반 로그인/회원가입
                .requestMatchers("/auth/refresh").permitAll() // 토큰 갱신
                .requestMatchers("/auth/kakao/login").permitAll() // 카카오 로그인
                .requestMatchers("/auth/kakao/login-url").permitAll() // 카카오 로그인 URL 생성
                // 카카오 가입 완료는 로그인한 사용자가 자기 계정에 하는 동작이다(대상은 토큰의 이메일).
                // permitAll 로 두면 JWT 필터 예외와 겹쳐 인증 없이 들어오고, 컨트롤러가 401 을 냈다.
                .requestMatchers("/auth/kakao/complete-registration").authenticated()
                
                // OAuth2 authorize/token (Spring Client beans).
                .requestMatchers("/oauth2/**").permitAll()
                .requestMatchers("/kakao-callback.html").permitAll()
                
                // 이메일 인증 관련 엔드포인트 (공개 접근)
                //
                // 주의: 이 규칙은 오랫동안 /users/send-code 등을 가리키고 있었는데, 실제 엔드포인트는
                // AuthController 의 /auth/* 다. 존재하지 않는 경로를 열어두고 진짜 경로는 아래
                // anyRequest().authenticated() 에 걸려 있어서, 가입 전 인증코드 발송과
                // 메일로 받은 인증 링크 클릭이 전부 401 이었다. 경로를 실제 매핑에 맞춘다.
                .requestMatchers(HttpMethod.POST, "/auth/send-code", "/auth/verify-code").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/verify").permitAll()

                // 관리자 API (ADMIN 권한 필요).
                // /api/** 인증 규칙보다 반드시 먼저 선언해야 한다.
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
                // 시설 상세·리뷰·검색·예측은 공공데이터와 공개 리뷰뿐이라 비로그인에도 연다.
                // 프런트는 이 화면들을 로그인 전에도 보여 주는데, 전에는 여기서 401 이 나 빈 화면이었다.
                // GET /facilities/* 는 한 세그먼트라 /facilities/bookings/{id} 같은 예약 경로에는 걸리지 않는다.
                .requestMatchers(HttpMethod.GET, "/facilities/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/facilities/recommend/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/facilities/*/reviews",
                        "/facilities/*/with-reviews",
                        "/facilities/*/admission-forecast",
                        "/facilities/*/popularity",
                        "/facilities/*/waitlist/stats").permitAll()
                .requestMatchers(HttpMethod.POST, "/facilities/search", "/facilities/advanced-search").permitAll()
                
                // 돌봄시설 공공데이터 API — 조회만 공개다.
                //
                // 동기화는 외부 공공데이터 API 를 페이지 단위로 호출하고 DB 에 쓴다.
                // 공개로 두면 누구나 공공데이터 일일 한도를 태우고 DB 를 두드릴 수 있다.
                // (이 프로젝트는 "공공데이터 한도 초과" 를 운영 알림으로 잡고 있는데,
                //  그 상황을 외부에서 마음대로 만들 수 있는 셈이다.)
                // swagger/sync 는 GET 이라 브라우저 접속이나 크롤러만으로도 실행된다.
                //
                // 같은 기능이 POST /api/admin/public-data/facilities/sync 로 이미 있다.
                .requestMatchers("/api/public/care-facilities/sync-all").hasRole("ADMIN")
                .requestMatchers("/api/public/care-facilities/swagger/sync").hasRole("ADMIN")
                .requestMatchers("/api/public/care-facilities/**").permitAll()
                
                // 병원 조회는 로그인 전에도 보여야 한다. 실제 경로가 /health/hospitals/** 라
                // 아래 /health/** 규칙보다 먼저 선언해야 한다.
                .requestMatchers(HttpMethod.GET, "/health/hospitals").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/nearby").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/popular").permitAll()
                .requestMatchers(HttpMethod.GET, "/health/hospitals/type/*").permitAll()
                // "내가 찜한 병원" 은 개인 목록이다. 경로가 한 세그먼트라 바로 아래
                // /health/hospitals/* 와일드카드에 먼저 걸리므로 그보다 앞에 선언해야 한다.
                // (병원 상세 /health/hospitals/{id} 와 같은 모양이라 눈에 잘 띄지 않는다.)
                .requestMatchers(HttpMethod.GET, "/health/hospitals/likes").authenticated()
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
                // "내가 좋아요/북마크한 글" 은 개인 목록이다. 경로가 한 세그먼트라
                // 아래 게시글 상세 와일드카드에 먼저 걸리므로 그보다 앞에 선언한다.
                // (현재는 컨트롤러가 현재 사용자를 다시 확인해 401 을 내지만,
                //  나중에 userId 파라미터를 받도록 바뀌면 그대로 남의 목록이 열린다.)
                .requestMatchers(HttpMethod.GET, "/community/posts/liked").authenticated()
                .requestMatchers(HttpMethod.GET, "/community/posts/bookmarked").authenticated()
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
                // /users/** 는 전부 "본인 계정" API 다. 남의 계정을 다루는 관리 기능은
                // /api/admin/users 로 옮겼으므로 여기에는 인증만 요구하면 충분하다.
                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/children/**").authenticated()
                .requestMatchers("/chatbot/**").authenticated()
                .requestMatchers("/api/**").authenticated()
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