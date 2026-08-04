package com.carecode.core.client.sync;

import com.carecode.core.client.XmlResponseParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** 공공데이터 페이징 수집 공통 절차. 데이터셋별 차이는 SyncSpec 으로 받는다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagedSyncTemplate {

    private final ObjectMapper objectMapper;
    private final XmlResponseParser xmlResponseParser;

    /** 무한 루프 방지 상한. 도달하면 중단 사유를 남긴다 — 조용한 중단은 하지 않는다. */
    @Value("${public.data.sync.max-pages:200}")
    private int maxPages;

    public SyncResult run(SyncSpec spec) {
        SyncResult result = new SyncResult(spec.getProvider().getProviderName(), spec.getLabel());

        if (!spec.getProvider().isAvailable()) {
            result.stop("서비스 키 미설정");
            log.info("{} 동기화 건너뜀 - 서비스 키가 없습니다.", spec.getLabel());
            return result;
        }

        for (int page = 1; page <= maxPages; page++) {
            JsonNode rows;
            try {
                String body = spec.getProvider()
                        .fetch(spec.getResource(), page, spec.getRowsPerPage(), spec.getParams());
                rows = extractRows(body);
            } catch (Exception e) {
                log.error("{} 조회 실패 - page={}", spec.getLabel(), page, e);
                result.stop("페이지 " + page + " 조회 실패: " + e.getMessage());
                return result;
            }

            if (rows == null || !rows.isArray() || rows.isEmpty()) {
                return result; // 더 이상 데이터 없음 — 정상 종료
            }
            if (page == 1) {
                logFieldNames(spec.getLabel(), rows.get(0));
            }

            int attempted = 0;
            int failedInPage = 0;
            for (JsonNode row : rows) {
                if (!spec.getFilter().test(row)) {
                    result.countSkipped();
                    continue;
                }
                attempted++;
                try {
                    if (Boolean.TRUE.equals(spec.getUpsert().apply(row))) {
                        result.countCreated();
                    } else {
                        result.countUpdated();
                    }
                } catch (Exception e) {
                    // 한 건 실패가 배치 전체를 중단시키지 않는다.
                    result.countFailed();
                    failedInPage++;
                    log.warn("{} 저장 실패: {}", spec.getLabel(), e.getMessage());
                }
            }
            result.countPage();

            // 첫 페이지가 전멸이면 응답 스펙이 바뀐 것이다. 200페이지를 헛돌지 않고 멈춘다.
            if (page == 1 && attempted > 0 && failedInPage == attempted) {
                result.stop("첫 페이지 전건 실패 - 응답 필드 매핑 불일치 의심");
                log.error("{} 동기화 중단 - 첫 페이지 {}건이 모두 실패했습니다. 응답 필드명을 확인하세요.",
                        spec.getLabel(), attempted);
                return result;
            }

            if (rows.size() < spec.getRowsPerPage()) {
                return result; // 마지막 페이지
            }
        }

        result.stop("최대 페이지(" + maxPages + ") 도달 - 남은 데이터가 있을 수 있습니다.");
        log.warn("{} 동기화가 페이지 상한에 걸렸습니다. public.data.sync.max-pages 설정을 확인하세요.", spec.getLabel());
        return result;
    }

    /** 매핑이 어긋났을 때 원인을 바로 찾을 수 있도록 실제 응답 필드명을 남긴다. */
    private void logFieldNames(String label, JsonNode firstRow) {
        if (firstRow == null || !log.isDebugEnabled()) {
            return;
        }
        List<String> names = new ArrayList<>();
        firstRow.fieldNames().forEachRemaining(names::add);
        log.debug("{} 응답 필드: {}", label, names);
    }

    /** 공공데이터 응답은 래핑 구조가 제각각이라 알려진 위치를 순서대로 확인한다. */
    private JsonNode extractRows(String body) {
        JsonNode root = parseBody(body);
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }

        // XML 로 받으면 최상위 response 가 벗겨져 body 부터 시작한다.
        for (JsonNode items : List.of(
                root.path("response").path("body").path("items"),
                root.path("body").path("items"),
                root.path("data"),
                root.path("items"))) {
            JsonNode rows = toArray(items);
            if (rows != null) {
                return rows;
            }
        }
        return null;
    }

    /** items 가 배열이거나, item 을 한 겹 더 감싸거나, 단건이면 객체로 온다. */
    private JsonNode toArray(JsonNode items) {
        if (items.isArray()) {
            return items;
        }
        JsonNode item = items.path("item");
        if (item.isArray()) {
            return item;
        }
        if (item.isObject()) {
            return objectMapper.createArrayNode().add(item);
        }
        return null;
    }

    /** JSON 을 먼저 시도하고, 아니면 XML 로 파싱한다. 데이터셋마다 기본 포맷이 다르다. */
    private JsonNode parseBody(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return objectMapper.readTree(trimmed);
            } catch (Exception e) {
                log.debug("JSON 파싱 실패, XML 로 재시도합니다: {}", e.getMessage());
            }
        }
        return xmlResponseParser.parse(trimmed);
    }
}
