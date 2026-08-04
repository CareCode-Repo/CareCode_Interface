package com.carecode.core.client.sync;

import com.carecode.core.client.XmlResponseParser;
import com.carecode.core.client.provider.DataGoKrProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 소아청소년과 병원 동기화.
 *
 * <p>출처: 건강보험심사평가원_병원정보서비스 (공공데이터포털)
 *
 * <p>{@code Hospital} 엔티티는 있었으나 데이터를 넣을 경로가 없어 비어 있었다.
 * 진료과목 코드로 소아청소년과만 걸러 적재한다.
 *
 * <p>이 데이터셋은 XML 로 응답하므로 {@link XmlResponseParser} 를 거친다.
 * (다른 data.go.kr 데이터셋과 달리 JSON 을 지원하지 않을 수 있어 두 포맷을 모두 처리한다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PediatricHospitalSyncService {

    private static final String RESOURCE = "B551182/hospInfoServicev2/getHospBasisList";

    private static final int ROWS_PER_PAGE = 100;

    private final DataGoKrProvider provider;
    private final HospitalUpsertService upsertService;
    private final XmlResponseParser xmlResponseParser;
    private final ObjectMapper objectMapper;

    /**
     * 진료과목 코드. 심평원 코드표 기준 소아청소년과는 "10".
     * 코드가 개정될 수 있어 설정으로 뺀다.
     */
    @Value("${public.data.hospital.pediatric-subject-code:10}")
    private String pediatricSubjectCode;

    @Value("${public.data.hospital.subject-name:소아청소년과}")
    private String subjectName;

    @Value("${public.data.sync.max-pages:200}")
    private int maxPages;

    public SyncResult sync() {
        SyncResult result = new SyncResult(provider.getProviderName(), "소아청소년과병원");

        if (!provider.isAvailable()) {
            result.stop("공공데이터포털 서비스 키 미설정");
            log.info("병원 동기화 건너뜀 - 서비스 키가 없습니다.");
            return result;
        }

        for (int page = 1; page <= maxPages; page++) {
            JsonNode rows;
            try {
                String body = provider.fetch(RESOURCE, page, ROWS_PER_PAGE, buildParams());
                rows = extractRows(body);
            } catch (Exception e) {
                log.error("병원 정보 조회 실패 - page={}", page, e);
                result.stop("페이지 " + page + " 조회 실패: " + e.getMessage());
                return result;
            }

            if (rows == null || rows.isEmpty()) {
                return result;
            }

            int rowCount = 0;
            for (JsonNode row : rows) {
                rowCount++;
                try {
                    if (upsertService.upsert(row, subjectName)) {
                        result.countCreated();
                    } else {
                        result.countUpdated();
                    }
                } catch (Exception e) {
                    result.countFailed();
                    log.warn("병원 저장 실패: {}", e.getMessage());
                }
            }
            result.countPage();

            if (rowCount < ROWS_PER_PAGE) {
                return result;
            }
        }

        result.stop("최대 페이지(" + maxPages + ") 도달 - 남은 데이터가 있을 수 있습니다.");
        log.warn("병원 동기화가 페이지 상한에 걸렸습니다.");
        return result;
    }

    private Map<String, String> buildParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("dgsbjtCd", pediatricSubjectCode);
        // 이 데이터셋은 XML 이 기본이다. JSON 을 지원하면 _type 으로 받고, 아니면 XML 로 파싱한다.
        params.put("_type", "json");
        return params;
    }

    /**
     * 응답에서 항목 배열을 꺼낸다. JSON 과 XML 응답을 모두 처리한다.
     *
     * <p>단일 항목이면 배열이 아니라 객체로 오는 경우가 있어(특히 XML→JSON 변환 시)
     * 그 경우 1건짜리 배열로 감싼다.
     */
    private JsonNode extractRows(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        JsonNode root = null;
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                root = objectMapper.readTree(body);
            } catch (Exception ignored) {
                // JSON 파싱 실패 시 XML 로 재시도
            }
        }
        if (root == null) {
            root = xmlResponseParser.parse(body);
        }
        if (root == null) {
            return null;
        }

        JsonNode items = root.path("body").path("items");
        if (items.isMissingNode()) {
            items = root.path("response").path("body").path("items");
        }

        JsonNode item = items.path("item");
        if (item.isArray()) {
            return item;
        }
        if (item.isObject()) {
            return objectMapper.createArrayNode().add(item);
        }
        if (items.isArray()) {
            return items;
        }
        return null;
    }
}
