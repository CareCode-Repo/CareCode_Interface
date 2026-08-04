package com.carecode.core.client.sync;

import com.carecode.core.client.provider.DataGoKrProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 정부 지원 서비스(보조금24) 동기화.
 *
 * <p>출처: 행정안전부_대한민국 공공서비스(혜택) 정보 (공공데이터포털)
 *
 * <p>정책 데이터가 코드에 하드코딩돼 있어 매년 바뀌는 내용을 재배포 없이는 반영할 수 없었다.
 * 중앙부처·지자체 서비스를 주기적으로 받아 자동 최신화한다.
 *
 * <p>약 7,500개 서비스 전부가 육아와 관련되지는 않으므로 키워드로 걸러 적재한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentBenefitSyncService {

    private static final String RESOURCE = "1741000/publicServiceInformations/publicServiceInformation";

    private static final int ROWS_PER_PAGE = 100;

    /** 육아 관련 서비스만 적재하기 위한 키워드. 하나라도 포함되면 대상으로 본다. */
    private static final List<String> CARE_KEYWORDS = List.of(
            "육아", "출산", "임신", "보육", "양육", "아동", "어린이", "영유아",
            "유아", "산모", "신생아", "돌봄", "child", "어린이집", "유치원");

    private final DataGoKrProvider provider;
    private final PolicyUpsertService upsertService;
    private final ObjectMapper objectMapper;

    @Value("${public.data.sync.max-pages:200}")
    private int maxPages;

    public SyncResult sync() {
        SyncResult result = new SyncResult(provider.getProviderName(), "정부지원서비스");

        if (!provider.isAvailable()) {
            result.stop("공공데이터포털 서비스 키 미설정");
            log.info("정부 지원 서비스 동기화 건너뜀 - 서비스 키가 없습니다.");
            return result;
        }

        for (int page = 1; page <= maxPages; page++) {
            JsonNode rows;
            try {
                String body = provider.fetch(RESOURCE, page, ROWS_PER_PAGE, buildParams());
                rows = extractRows(body);
            } catch (Exception e) {
                log.error("정부 지원 서비스 조회 실패 - page={}", page, e);
                result.stop("페이지 " + page + " 조회 실패: " + e.getMessage());
                return result;
            }

            if (rows == null || !rows.isArray() || rows.isEmpty()) {
                return result;
            }

            for (JsonNode row : rows) {
                if (!isCareRelated(row)) {
                    continue; // 육아와 무관한 서비스는 건너뛴다
                }
                try {
                    if (upsertService.upsert(row)) {
                        result.countCreated();
                    } else {
                        result.countUpdated();
                    }
                } catch (Exception e) {
                    result.countFailed();
                    log.warn("정책 저장 실패: {}", e.getMessage());
                }
            }
            result.countPage();

            if (rows.size() < ROWS_PER_PAGE) {
                return result;
            }
        }

        result.stop("최대 페이지(" + maxPages + ") 도달 - 남은 데이터가 있을 수 있습니다.");
        log.warn("정부 지원 서비스 동기화가 페이지 상한에 걸렸습니다.");
        return result;
    }

    private Map<String, String> buildParams() {
        // 데이터셋이 연령·가구 조건 필터를 명세에 노출하지 않아, 전체를 받아 키워드로 거른다.
        // 서버 측 필터가 확인되면 이 자리에서 파라미터로 좁히는 편이 트래픽에 유리하다.
        return new LinkedHashMap<>();
    }

    /** 서비스명·요약·분야 중 하나라도 육아 키워드를 포함하면 적재 대상. */
    private boolean isCareRelated(JsonNode row) {
        String haystack = String.join(" ",
                nullToEmpty(text(row, "서비스명", "servNm", "SVC_NM")),
                nullToEmpty(text(row, "서비스목적요약", "servDgst", "SVC_DGST")),
                nullToEmpty(text(row, "서비스분야", "srvPvsnNm", "INTRS_THEMA_NM")),
                nullToEmpty(text(row, "지원대상", "trgterIndvdlArray")))
                .toLowerCase();

        return CARE_KEYWORDS.stream().anyMatch(k -> haystack.contains(k.toLowerCase()));
    }

    private JsonNode extractRows(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(body);

        JsonNode data = root.path("data");
        if (data.isArray()) {
            return data;
        }
        JsonNode items = root.path("response").path("body").path("items");
        if (items.isArray()) {
            return items;
        }
        if (items.path("item").isArray()) {
            return items.path("item");
        }
        return root.isArray() ? root : null;
    }

    private String text(JsonNode row, String... keys) {
        for (String key : keys) {
            JsonNode node = row.get(key);
            if (node != null && !node.isNull()) {
                String value = node.asText().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
