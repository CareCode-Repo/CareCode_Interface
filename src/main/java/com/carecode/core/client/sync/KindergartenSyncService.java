package com.carecode.core.client.sync;

import com.carecode.core.client.provider.KindergartenInfoProvider;
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
 * 전국 유치원 동기화.
 * 이 API 는 sggCode 가 필수라 페이지가 아니라 시군구를 순회한다 — PagedSyncTemplate 을 쓸 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KindergartenSyncService {

    private final KindergartenInfoProvider provider;
    private final KindergartenUpsertService upsertService;
    private final RegionCodeCatalog regionCatalog;
    private final ObjectMapper objectMapper;

    /** basicInfo2 는 basicInfo 와 달리 정원·위경도를 준다. 예측 기능이 이 값에 의존한다. */
    @Value("${public.data.resource.kindergarten:"
            + "https://e-childschoolinfo.moe.go.kr/api/notice/basicInfo2.do}")
    private String resource;

    public SyncResult sync() {
        SyncResult result = new SyncResult(provider.getProviderName(), "전국유치원");

        if (!provider.isAvailable()) {
            result.stop("유치원알리미 서비스 키 미설정");
            log.info("유치원 동기화 건너뜀 - 서비스 키가 없습니다.");
            return result;
        }

        List<RegionCodeCatalog.RegionCode> regions = regionCatalog.kindergartenRegions();
        if (regions.isEmpty()) {
            result.stop("시군구 코드 목록이 비어 있음");
            return result;
        }

        int emptyRegions = 0;
        for (RegionCodeCatalog.RegionCode region : regions) {
            JsonNode rows;
            try {
                rows = extractRows(provider.fetch(resource, 1, 0, buildParams(region)));
            } catch (Exception e) {
                // 한 지역 실패로 전국 수집을 중단하지 않는다.
                log.warn("유치원 조회 실패 - {}/{}: {}", region.sidoCode(), region.sggCode(), e.getMessage());
                result.countFailed();
                continue;
            }

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
                    log.warn("유치원 저장 실패: {}", e.getMessage());
                }
            }
            result.countPage();
        }

        // 전 지역이 비었다면 키가 막혔거나 응답 스펙이 바뀐 것이다.
        if (emptyRegions == regions.size()) {
            result.stop("전 지역 응답 없음 - 서비스 키 또는 응답 형식 확인 필요");
            log.error("유치원 동기화: {}개 지역 전부 빈 응답", regions.size());
        }
        return result;
    }

    private Map<String, String> buildParams(RegionCodeCatalog.RegionCode region) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("sidoCode", region.sidoCode());
        params.put("sggCode", region.sggCode());
        return params;
    }

    private JsonNode extractRows(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return null;
        }
        JsonNode root = objectMapper.readTree(body);
        JsonNode rows = root.path("kinderInfo");
        return rows.isArray() ? rows : null;
    }
}
