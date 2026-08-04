package com.carecode.core.client.sync;

import com.carecode.core.client.XmlResponseParser;
import com.carecode.core.client.provider.PublicDataProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("공공데이터 페이징 동기화")
class PagedSyncTemplateTest {

    private PagedSyncTemplate template;

    @BeforeEach
    void setUp() {
        template = new PagedSyncTemplate(new ObjectMapper(), new XmlResponseParser());
        ReflectionTestUtils.setField(template, "maxPages", 5);
    }

    @Test
    @DisplayName("서비스 키가 없으면 호출하지 않고 사유를 남긴다")
    void skipsWhenProviderUnavailable() {
        StubProvider provider = new StubProvider(false, List.of());

        SyncResult result = run(provider, row -> true);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getStoppedReason()).contains("서비스 키");
        assertThat(provider.callCount).isZero();
    }

    @Test
    @DisplayName("요청 건수보다 적게 오면 마지막 페이지로 보고 더 호출하지 않는다")
    void stopsOnShortPage() {
        StubProvider provider = new StubProvider(true, List.of(page(1)));

        SyncResult result = run(provider, row -> true);

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getPagesProcessed()).isEqualTo(1);
        assertThat(provider.callCount).isEqualTo(1);
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("빈 응답이 오면 정상 종료한다")
    void stopsOnEmptyResponse() {
        StubProvider provider = new StubProvider(true, List.of(page(2)));

        SyncResult result = run(provider, row -> true);

        assertThat(result.getCreated()).isEqualTo(2);
        assertThat(result.isCompleted()).isTrue();
        assertThat(provider.callCount).isEqualTo(2); // 두 번째 호출에서 데이터 없음을 확인
    }

    @Test
    @DisplayName("첫 페이지가 전건 실패면 매핑 불일치로 보고 즉시 중단한다")
    void stopsWhenFirstPageEntirelyFails() {
        StubProvider provider = new StubProvider(true, List.of(page(3), page(3)));

        SyncResult result = template.run(SyncSpec.builder()
                .provider(provider)
                .resource("test")
                .label("테스트")
                .rowsPerPage(3)
                .upsert(row -> {
                    throw new IllegalArgumentException("필수 코드 없음");
                })
                .build());

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getStoppedReason()).contains("매핑 불일치");
        assertThat(result.getFailed()).isEqualTo(3);
        assertThat(provider.callCount).isEqualTo(1); // 나머지 페이지를 헛돌지 않는다
    }

    @Test
    @DisplayName("일부만 실패하면 계속 진행한다")
    void continuesOnPartialFailure() {
        StubProvider provider = new StubProvider(true, List.of(page(3)));
        List<Integer> seen = new ArrayList<>();

        SyncResult result = template.run(SyncSpec.builder()
                .provider(provider)
                .resource("test")
                .label("테스트")
                .rowsPerPage(3)
                .upsert(row -> {
                    int id = row.get("id").asInt();
                    seen.add(id);
                    if (id == 1) {
                        throw new IllegalStateException("한 건 실패");
                    }
                    return true;
                })
                .build());

        assertThat(seen).hasSize(3);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getCreated()).isEqualTo(2);
        assertThat(result.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("필터에 걸린 행은 실패가 아니라 제외로 센다")
    void countsFilteredRowsAsSkipped() {
        StubProvider provider = new StubProvider(true, List.of(page(4)));

        SyncResult result = template.run(SyncSpec.builder()
                .provider(provider)
                .resource("test")
                .label("테스트")
                .rowsPerPage(4)
                .filter(row -> row.get("id").asInt() % 2 == 0)
                .upsert(row -> true)
                .build());

        assertThat(result.getSkipped()).isEqualTo(2);
        assertThat(result.getCreated()).isEqualTo(2);
        assertThat(result.getFailed()).isZero();
    }

    @Test
    @DisplayName("전건 제외는 매핑 불일치로 보지 않는다")
    void doesNotStopWhenAllRowsFiltered() {
        StubProvider provider = new StubProvider(true, List.of(page(3)));

        SyncResult result = template.run(SyncSpec.builder()
                .provider(provider)
                .resource("test")
                .label("테스트")
                .rowsPerPage(3)
                .filter(row -> false)
                .upsert(row -> true)
                .build());

        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getSkipped()).isEqualTo(3);
    }

    @Test
    @DisplayName("XML 로 응답해도 항목을 읽는다")
    void parsesXmlResponse() {
        String xml = """
                <response><body><items>
                  <item><id>1</id></item>
                  <item><id>2</id></item>
                </items></body></response>
                """;
        StubProvider provider = new StubProvider(true, List.of(xml));

        SyncResult result = run(provider, row -> true);

        assertThat(result.getCreated()).isEqualTo(2);
    }

    @Test
    @DisplayName("페이지 상한에 걸리면 조용히 끝내지 않고 사유를 남긴다")
    void reportsPageLimit() {
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            pages.add(page(2));
        }
        StubProvider provider = new StubProvider(true, pages);

        SyncResult result = run(provider, row -> true);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getStoppedReason()).contains("최대 페이지");
        assertThat(result.getPagesProcessed()).isEqualTo(5);
    }

    @Test
    @DisplayName("조회 실패는 사유와 함께 중단한다")
    void stopsOnFetchFailure() {
        StubProvider provider = new StubProvider(true, List.of()) {
            @Override
            public String fetch(String resource, int pageNo, int numOfRows, Map<String, String> params) {
                throw new IllegalStateException("연결 시간 초과");
            }
        };

        SyncResult result = run(provider, row -> true);

        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getStoppedReason()).contains("연결 시간 초과");
    }

    private SyncResult run(PublicDataProvider provider, Function<JsonNode, Boolean> upsert) {
        return template.run(SyncSpec.builder()
                .provider(provider)
                .resource("test")
                .label("테스트")
                .rowsPerPage(2)
                .upsert(upsert)
                .build());
    }

    /** id 만 담은 행 n 건짜리 페이지 JSON. */
    private String page(int rowCount) {
        StringBuilder sb = new StringBuilder("{\"response\":{\"body\":{\"items\":[");
        for (int i = 0; i < rowCount; i++) {
            sb.append(i > 0 ? "," : "").append("{\"id\":").append(i).append("}");
        }
        return sb.append("]}}}").toString();
    }

    /** 페이지를 순서대로 돌려주고, 다 떨어지면 빈 응답을 준다. */
    private static class StubProvider implements PublicDataProvider {
        private final boolean available;
        private final List<String> pages;
        private int callCount;

        StubProvider(boolean available, List<String> pages) {
            this.available = available;
            this.pages = pages;
        }

        @Override
        public String getProviderName() {
            return "STUB";
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String fetch(String resource, int pageNo, int numOfRows, Map<String, String> params) {
            callCount++;
            return pageNo <= pages.size() ? pages.get(pageNo - 1) : null;
        }
    }
}
