package com.carecode.core.client.sync;

import com.carecode.core.client.provider.DataGoKrProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/** 전국 어린이집 정보 동기화. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NationwideChildcareFacilitySyncService {

    /** 공공데이터포털 오퍼레이션 경로. 데이터셋 문서의 상세 기능 경로와 맞춘다. */
    private static final String RESOURCE = "B551014/CCEF/childcare";

    private static final int ROWS_PER_PAGE = 500;

    private final DataGoKrProvider provider;
    private final CareFacilityUpsertService upsertService;
    private final ObjectMapper objectMapper;

    /** 무한 루프 방지 상한. 도달하면 중단 사유를 남긴다(조용한 중단 금지). */
    @Value("${public.data.sync.max-pages:200}")
    private int maxPages;

    public SyncResult sync() {
        SyncResult result = new SyncResult(provider.getProviderName(), "전국어린이집");

        if (!provider.isAvailable()) {
            result.stop("공공데이터포털 서비스 키 미설정");
            log.info("전국 어린이집 동기화 건너뜀 - 서비스 키가 없습니다.");
            return result;
        }

        for (int page = 1; page <= maxPages; page++) {
            JsonNode rows;
            try {
                String body = provider.fetch(RESOURCE, page, ROWS_PER_PAGE, Map.of());
                rows = extractRows(body);
            } catch (Exception e) {
                log.error("전국 어린이집 조회 실패 - page={}", page, e);
                result.stop("페이지 " + page + " 조회 실패: " + e.getMessage());
                return result;
            }

            if (rows == null || !rows.isArray() || rows.isEmpty()) {
                return result; // 더 이상 데이터 없음 — 정상 종료
            }

            for (JsonNode row : rows) {
                try {
                    if (upsertService.upsert(row)) {
                        result.countCreated();
                    } else {
                        result.countUpdated();
                    }
                } catch (Exception e) {
                    // 한 건 실패가 배치 전체를 중단시키지 않는다.
                    result.countFailed();
                    log.warn("시설 저장 실패: {}", e.getMessage());
                }
            }
            result.countPage();

            if (rows.size() < ROWS_PER_PAGE) {
                return result; // 마지막 페이지
            }
        }

        result.stop("최대 페이지(" + maxPages + ") 도달 - 남은 데이터가 있을 수 있습니다.");
        log.warn("전국 어린이집 동기화가 페이지 상한에 걸렸습니다. public.data.sync.max-pages 설정을 확인하세요.");
        return result;
    }

    /** 공공데이터포털 응답에서 데이터 배열을 꺼낸다. */
    private JsonNode extractRows(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(body);

        JsonNode items = root.path("response").path("body").path("items");
        if (items.isArray()) {
            return items;
        }
        if (items.path("item").isArray()) {
            return items.path("item");
        }
        if (root.path("items").isArray()) {
            return root.path("items");
        }
        return root.isArray() ? root : null;
    }
}
