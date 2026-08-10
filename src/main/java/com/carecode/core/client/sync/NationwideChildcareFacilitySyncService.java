package com.carecode.core.client.sync;

import com.carecode.core.client.XmlResponseParser;
import com.carecode.core.client.provider.ChildcarePortalProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 전국 어린이집 동기화.
 * arcode(시군구) 가 필수이고 페이징이 없어 지역을 순회한다 — PagedSyncTemplate 을 쓸 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NationwideChildcareFacilitySyncService {

    private final ChildcarePortalProvider provider;
    private final CareFacilityUpsertService upsertService;
    private final RegionCodeCatalog regionCatalog;
    private final XmlResponseParser xmlResponseParser;

    @Value("${public.data.resource.childcare:"
            + "https://api.childcare.go.kr/mediate/rest/cpmsapi021/cpmsapi021/request}")
    private String resource;

    public SyncResult sync() {
        SyncResult result = new SyncResult(provider.getProviderName(), "전국어린이집");

        if (!provider.isAvailable()) {
            result.stop("보육통합정보 서비스 키 미설정");
            log.info("어린이집 동기화 건너뜀 - 서비스 키가 없습니다.");
            return result;
        }

        List<String> regions = regionCatalog.childcareRegions();
        if (regions.isEmpty()) {
            result.stop("시군구 코드 목록이 비어 있음");
            return result;
        }

        int emptyRegions = 0;
        for (String arcode : regions) {
            JsonNode root;
            try {
                root = parse(provider.fetch(resource, 1, 0, buildParams(arcode)));
            } catch (Exception e) {
                // 한 지역 실패로 전국 수집을 중단하지 않는다.
                log.warn("어린이집 조회 실패 - arcode={}: {}", arcode, e.getMessage());
                result.countFailed();
                continue;
            }

            // 한도 초과·키 만료를 "검색결과 없음" 으로 넘기면 조용히 0건으로 끝난다.
            ChildcareApiStatus status = ChildcareApiStatus.of(root);
            if (status.isFatal()) {
                result.stop("API 응답: " + status.describe(root));
                log.error("어린이집 동기화 중단 - {}", status.describe(root));
                return result;
            }
            if (status == ChildcareApiStatus.MISSING_PARAM || status == ChildcareApiStatus.SERVER_ERROR) {
                log.warn("어린이집 조회 오류 - arcode={}, {}", arcode, status.describe(root));
                result.countFailed();
                continue;
            }

            JsonNode rows = extractRows(root);
            if (rows == null || rows.isEmpty()) {
                emptyRegions++;
                continue;
            }
            for (JsonNode row : rows) {
                try {
                    if (upsertService.upsert(row)) {
                        result.countCreated();
                    } else {
                        result.countUpdated();
                    }
                } catch (Exception e) {
                    result.countFailed();
                    log.warn("시설 저장 실패: {}", e.getMessage());
                }
            }
            result.countPage();
        }

        if (emptyRegions == regions.size()) {
            result.stop("전 지역 응답 없음 - 서비스 키 또는 응답 형식 확인 필요");
            log.error("어린이집 동기화: {}개 지역 전부 빈 응답", regions.size());
        }
        return result;
    }

    private Map<String, String> buildParams(String arcode) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("arcode", arcode);
        return params;
    }

    private JsonNode parse(String body) {
        return body == null || body.isBlank() ? null : xmlResponseParser.parse(body);
    }

    /** 응답은 XML 이고 항목이 response/item 으로 온다. */
    private JsonNode extractRows(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode items = root.path("item");
        return items.isArray() ? items : null;
    }
}
