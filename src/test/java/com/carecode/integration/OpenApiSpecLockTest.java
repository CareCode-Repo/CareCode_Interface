package com.carecode.integration;

import com.carecode.CareCodeApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * API 스펙을 파일로 고정한다 (`docs/api/openapi.json`).
 *
 * <p>프런트와 서버가 어긋나 조용히 깨지는 일이 반복됐다. 서버에서 지운 경로를 프런트가 계속 부르고,
 * 서버가 보내지 않는 필드를 프런트가 필수로 요구했다. 둘 다 배포 후 화면에서야 드러났다.
 *
 * <p>스펙을 저장소에 두면 (1) 프런트가 서버를 띄우지 않고도 대조할 수 있고,
 * (2) 경로·필드가 바뀌면 이 파일의 diff 로 리뷰에 드러난다. 파일을 갱신하지 않고 API 를 바꾸면
 * 이 테스트가 실패한다.
 *
 * <p>갱신: {@code ./gradlew test --tests '*OpenApiSpecLockTest' -Popenapi.lock.update=true}
 */
@SpringBootTest(
        classes = CareCodeApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
                "spring.cache.type=none",
                "spring.datasource.url=jdbc:h2:mem:carecode_openapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false",
                "jwt.secret=testJwtSecretKeyForAccessControlTestMustBe256BitsLong0123456789",
                // 스펙을 뽑아야 하므로 문서 기능은 켠다 (운영 프로파일에서는 차단된다).
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=false",
                // 서버 주소는 환경마다 다르다. 스펙 파일이 환경 때문에 바뀌지 않게 고정한다.
                "springdoc.swagger-ui.server-url=http://localhost:8082",
                "public.data.api.key=dummy",
                "KAKAO_CLIENT_ID=dummy-kakao-client",
                "KAKAO_CLIENT_SECRET=dummy-kakao-secret",
                "MAIL_USERNAME=dummy",
                "MAIL_PASSWORD=dummy"
        }
)
@AutoConfigureMockMvc
@DisplayName("API 스펙 고정")
class OpenApiSpecLockTest {

    private static final Path LOCK_FILE = Path.of("docs/api/openapi.json");
    private static final String UPDATE_FLAG = "openapi.lock.update";

    @MockBean RedisConnectionFactory redisConnectionFactory;
    @MockBean StringRedisTemplate stringRedisTemplate;
    @MockBean JavaMailSender javaMailSender;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("저장된 스펙이 현재 API 와 같다")
    void specMatchesLockFile() throws Exception {
        String current = fetchSpec();

        if (Boolean.parseBoolean(System.getProperty(UPDATE_FLAG))) {
            Files.createDirectories(LOCK_FILE.getParent());
            Files.writeString(LOCK_FILE, current, StandardCharsets.UTF_8);
            return;
        }

        assertThat(LOCK_FILE)
                .as("API 스펙 파일이 없습니다. -P%s=true 로 생성하세요.", UPDATE_FLAG)
                .exists();

        String saved = Files.readString(LOCK_FILE, StandardCharsets.UTF_8);
        assertThat(normalize(current))
                .as("""
                        API 가 바뀌었는데 스펙 파일(%s)이 갱신되지 않았습니다.
                        프런트는 이 파일을 보고 호출을 맞춥니다. 아래로 갱신한 뒤 함께 커밋하세요.
                          ./gradlew test --tests '*OpenApiSpecLockTest' -P%s=true
                        """.formatted(LOCK_FILE, UPDATE_FLAG))
                .isEqualTo(normalize(saved));
    }

    @Test
    @DisplayName("스펙에 주요 경로가 실제로 담긴다 — 빈 스펙을 고정해 두는 사고 방지")
    void specCoversKnownPaths() throws Exception {
        JsonNode paths = objectMapper.readTree(fetchSpec()).path("paths");

        assertThat(paths.size())
                .as("경로 수가 비정상적으로 적습니다. 스펙 생성이 실패했을 수 있습니다.")
                .isGreaterThan(100);
        assertThat(paths.has("/auth/login")).isTrue();
        assertThat(paths.has("/facilities/{facilityId}/admission-forecast")).isTrue();
        assertThat(paths.has("/notifications/stream")).isTrue();
    }

    private String fetchSpec() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(result.getResponse().getStatus()).as(body).isEqualTo(200);

        // springdoc 은 실행마다 키 순서를 다르게 낸다(빈 스캔 순서). 순서까지 고정해야 diff 가
        // "정말 바뀐 것" 만 보여 준다. ORDER_MAP_ENTRIES_BY_KEYS 는 JsonNode 에는 적용되지 않는다.
        ObjectMapper stable = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        return stable.writeValueAsString(sortKeys(stable.readTree(body))) + "\n";
    }

    /** 객체 키를 재귀적으로 정렬한다. 배열은 순서에 의미가 있을 수 있어 그대로 두고, 태그 목록만 이름순으로 맞춘다. */
    private static JsonNode sortKeys(JsonNode node) {
        if (node.isObject()) {
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            node.properties().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sorted.set(entry.getKey(), sortKeys(entry.getValue())));
            return sorted;
        }
        if (node.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            node.forEach(item -> items.add(sortKeys(item)));
            // 태그는 컨트롤러 스캔 순서라 실행마다 바뀐다. {name, description} 만 담긴 배열일 때만 정렬한다.
            boolean tagList = !items.isEmpty() && items.stream().allMatch(
                    item -> item.isObject() && item.has("name") && item.has("description") && item.size() == 2);
            if (tagList) {
                items.sort(Comparator.comparing(item -> item.get("name").asText()));
            }
            ArrayNode array = JsonNodeFactory.instance.arrayNode();
            items.forEach(array::add);
            return array;
        }
        return node;
    }

    /** 줄바꿈 방식(CRLF/LF)은 비교에서 제외한다. Windows 체크아웃에서 매번 실패하지 않게. */
    private static String normalize(String json) {
        return json.replace("\r\n", "\n").trim();
    }
}
