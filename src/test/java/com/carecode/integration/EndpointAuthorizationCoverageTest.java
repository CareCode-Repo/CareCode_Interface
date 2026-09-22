package com.carecode.integration;

import com.carecode.CareCodeApplication;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 모든 엔드포인트가 "의도적으로" 공개인지 확인한다.
 *
 * <p>이 저장소는 같은 유형의 사고를 네 번 겪었다.
 * <ul>
 *   <li>{@code /health/**} 가 {@code /hospitals/**} 를 삼켜 병원 공개 조회가 통째로 막힘</li>
 *   <li>이메일 인증 화이트리스트가 존재하지 않는 {@code /users/*} 를 가리켜 가입 흐름이 401</li>
 *   <li>{@code /health/hospitals/likes} 가 {@code /health/hospitals/*} 에 먹혀 개인 목록이 공개</li>
 *   <li>{@code /community/posts/liked|bookmarked} 가 게시글 상세 와일드카드에 먹힘</li>
 * </ul>
 *
 * <p>공통점은 <b>규칙 자체는 멀쩡해 보이는데 매칭 순서 때문에 의도와 다르게 동작</b>한다는 것이다.
 * 계약 테스트에 경로를 하나씩 적는 방식으로는 새 엔드포인트가 생길 때마다 놓친다.
 * 그래서 방향을 뒤집는다 — <b>모든 매핑은 기본적으로 인증이 필요하고, 공개는 아래 목록에
 * 적힌 것만</b>이다. 목록에 없는 경로가 공개로 열리면 이 테스트가 깨진다.
 *
 * <p>실제 요청을 보내지 않고 인가 판단만 평가한다. 컨트롤러가 실행되지 않으므로
 * DB 변경이나 외부 API 호출 같은 부작용이 없다.
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration",
                "spring.cache.type=none",
                "spring.batch.job.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:carecode_authcov;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForAuthorizationCoverageMustBe256BitsLong0123456789",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false",
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@DisplayName("엔드포인트 인가 커버리지")
class EndpointAuthorizationCoverageTest {

    /**
     * 비로그인으로 열어야 하는 경로.
     *
     * <p>여기에 추가할 때는 "이 응답에 특정 사용자에게 속한 것이 하나라도 들어가는가" 를 묻는다.
     * 들어간다면 공개가 아니다. 목록은 {@code docs/reference/access-control-matrix.md} 와 짝을 이룬다.
     */
    private static final Set<String> INTENTIONALLY_PUBLIC = new TreeSet<>(Set.of(
            // 시스템·문서
            "GET /", "GET /error", "GET /favicon.ico",
            "GET /actuator/health", "GET /actuator/info", "GET /actuator/prometheus",
            "GET /legal/privacy-policy", "GET /legal/terms", "GET /legal/version",

            // 인증 흐름 (로그인 전에 호출된다)
            "POST /auth/login", "POST /auth/register", "POST /auth/refresh",
            "POST /auth/send-code", "POST /auth/verify-code", "GET /auth/verify",
            "POST /auth/kakao/login", "GET /auth/kakao/login-url",
            // /auth/kakao/complete-registration 은 공개가 아니다. 카카오 로그인이 준 토큰으로
            // 자기 계정의 가입을 끝내는 동작이다. 예전에 여기 공개로 적혀 있었고, 그래서
            // JWT 필터가 건너뛰어 흐름 전체가 401 이었다.

            // 둘러보기 — 로그인 전에 보여야 가입 전환이 생긴다
            "GET /facilities", "GET /facilities/popular", "GET /facilities/new",
            "GET /facilities/radius", "GET /facilities/statistics", "GET /facilities/age",
            "GET /facilities/operating-hours",

            "GET /policies", "GET /policies/categories", "GET /policies/statistics",
            "GET /policies/active", "GET /policies/age", "GET /policies/child-age",
            "GET /policies/latest", "GET /policies/popular", "POST /policies/search",

            "GET /health/hospitals", "GET /health/hospitals/nearby", "GET /health/hospitals/popular",

            "GET /community/posts", "GET /community/search", "GET /community/popular",
            "GET /community/latest", "GET /community/tags", "GET /community/search/all",
            "GET /community/popular/limit", "GET /community/latest/limit",

            // 공공데이터 조회 (동기화 트리거는 ADMIN 이다)
            "GET /api/public/care-facilities/swagger/stats",
            "GET /api/public/care-facilities/swagger/db-facilities",

            // 조회수 증가. 쓰기지만 비로그인 방문자의 조회도 세야 하므로 공개다.
            // 다만 이 값이 /facilities/popular 순위에 쓰이므로 부풀릴 수 있다.
            // 인기 순위를 조작에 민감하게 다뤄야 한다면 여기부터 손봐야 한다.
            "POST /facilities/1/view"
    ));

    /**
     * 경로 변수를 포함해 공개인 것들. 변수 자리를 채우면 문자열이 달라져
     * 위 목록으로는 표현할 수 없어 접두사로 둔다.
     */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "GET /facilities/type/",
            "GET /facilities/location/",
            "GET /policies/",              // 정책 상세
            "GET /facilities/",            // 시설 상세·조회수·평점
            "GET /health/hospitals/",      // 병원 상세·리뷰·좋아요 수
            "GET /community/posts/",       // 게시글 상세·댓글
            "GET /community/tags/",
            "GET /files/profile-images/"
    );

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    // actuator 가 같은 타입의 빈을 하나 더 등록한다(controllerEndpointHandlerMapping).
    @Autowired @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;
    @Autowired SecurityFilterChain securityFilterChain;

    private AuthorizationManager<HttpServletRequest> authorizationManager() {
        for (Filter filter : securityFilterChain.getFilters()) {
            if (filter instanceof AuthorizationFilter authorizationFilter) {
                return authorizationFilter.getAuthorizationManager();
            }
        }
        throw new IllegalStateException("AuthorizationFilter 를 찾지 못했습니다.");
    }

    private static Authentication anonymous() {
        return new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
    }

    /** 경로 변수 자리를 채운다. 인가는 값에 의존하지 않으므로 아무 값이나 된다. */
    private static String fill(String pattern) {
        return pattern.replaceAll("\\{[^/}]+}", "1");
    }

    private boolean isPublic(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        AuthorizationDecision decision =
                authorizationManager().check(EndpointAuthorizationCoverageTest::anonymous, request);
        return decision == null || decision.isGranted();
    }

    private static boolean allowed(String signature) {
        if (INTENTIONALLY_PUBLIC.contains(signature)) {
            return true;
        }
        return PUBLIC_PREFIXES.stream().anyMatch(signature::startsWith);
    }

    /**
     * 실제로 훑은 매핑 수의 하한.
     *
     * <p>이게 없으면 매핑을 하나도 못 읽었을 때도 "위반 없음" 으로 통과한다.
     * 조용히 아무것도 검사하지 않는 게이트는 없는 것보다 나쁘다 — 통과했다는 착각을 준다.
     * (CI 에도 같은 이유로 스키마 검증이 skip 되면 실패시키는 단계가 있다.)
     */
    private static final int MIN_EXAMINED_MAPPINGS = 100;

    @Test
    @DisplayName("목록에 없는 엔드포인트가 공개로 열려 있지 않다")
    void noUndeclaredPublicEndpoint() {
        Set<String> unexpected = new TreeSet<>();
        int examined = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry
                : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();

            Set<String> patterns = new LinkedHashSet<>();
            if (info.getPathPatternsCondition() != null) {
                info.getPathPatternsCondition().getPatterns()
                        .forEach(p -> patterns.add(p.getPatternString()));
            }
            if (patterns.isEmpty()) {
                continue;
            }

            Set<String> methods = new LinkedHashSet<>();
            info.getMethodsCondition().getMethods().forEach(m -> methods.add(m.name()));
            if (methods.isEmpty()) {
                methods.add("GET");
            }

            for (String pattern : patterns) {
                for (String method : methods) {
                    examined++;
                    String path = fill(pattern);
                    if (!isPublic(method, path)) {
                        continue;
                    }
                    String signature = method + " " + path;
                    if (!allowed(signature)) {
                        unexpected.add(signature + "   (매핑: " + pattern + ", "
                                + entry.getValue().getBeanType().getSimpleName() + ")");
                    }
                }
            }
        }

        assertThat(examined)
                .as("매핑을 읽지 못하면 이 테스트는 아무것도 검사하지 않은 채 통과한다")
                .isGreaterThanOrEqualTo(MIN_EXAMINED_MAPPINGS);

        assertThat(unexpected)
                .as("""
                        비로그인으로 열려 있는데 공개 목록에 없는 엔드포인트다.
                        둘 중 하나다.
                          (1) 정말 공개여야 한다  → INTENTIONALLY_PUBLIC 에 추가하고 근거를 남긴다
                          (2) 공개면 안 된다      → SecurityConfig 에서 와일드카드보다 앞에 규칙을 선언한다
                        """)
                .isEmpty();
    }

    @Test
    @DisplayName("공개 목록에 적어둔 경로는 실제로 열려 있다")
    void declaredPublicEndpointsAreReallyOpen() {
        Set<String> closed = new TreeSet<>();

        for (String signature : INTENTIONALLY_PUBLIC) {
            String[] parts = signature.split(" ", 2);
            if (!isPublic(parts[0], parts[1])) {
                closed.add(signature);
            }
        }

        // 목록이 현실과 어긋나면 그것도 버그다. 실제로 이메일 인증 경로가
        // "열어뒀다고 적혀 있는데 실은 막혀 있는" 상태로 오래 남아 있었다.
        assertThat(closed)
                .as("공개로 선언했지만 인증을 요구한다. 경로가 실제 매핑과 맞는지 확인하라")
                .isEmpty();
    }
}
