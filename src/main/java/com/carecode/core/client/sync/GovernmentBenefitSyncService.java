package com.carecode.core.client.sync;

import com.carecode.core.client.provider.OdcloudProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/** 정부 지원 서비스(보조금24) 동기화. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernmentBenefitSyncService {

    private static final int ROWS_PER_PAGE = 100;

    /** 육아 관련 서비스만 적재하기 위한 키워드. 하나라도 포함되면 대상으로 본다. */
    private static final List<String> CARE_KEYWORDS = List.of(
            "육아", "출산", "임신", "보육", "양육", "아동", "어린이", "영유아",
            "유아", "산모", "신생아", "돌봄", "child", "어린이집", "유치원");

    private final OdcloudProvider provider;
    private final PolicyUpsertService upsertService;
    private final PagedSyncTemplate syncTemplate;

    @Value("${public.data.resource.benefit:https://api.odcloud.kr/api/gov24/v3/serviceList}")
    private String resource;

    public SyncResult sync() {
        return syncTemplate.run(SyncSpec.builder()
                .provider(provider)
                .resource(resource)
                .label("정부지원서비스")
                .rowsPerPage(ROWS_PER_PAGE)
                .filter(this::isCareRelated)
                .upsert(upsertService::upsert)
                .build());
    }

    /** 서비스명·요약·분야 중 하나라도 육아 키워드를 포함하면 적재 대상. */
    private boolean isCareRelated(JsonNode row) {
        String haystack = String.join(" ",
                text(row, "서비스명", "servNm", "SVC_NM"),
                text(row, "서비스목적요약", "servDgst", "SVC_DGST"),
                text(row, "서비스분야", "srvPvsnNm", "INTRS_THEMA_NM"),
                text(row, "지원대상", "trgterIndvdlArray"))
                .toLowerCase();

        return CARE_KEYWORDS.stream().anyMatch(k -> haystack.contains(k.toLowerCase()));
    }

    private String text(JsonNode row, String... keys) {
        for (String key : keys) {
            JsonNode node = row.get(key);
            if (node != null && !node.isNull()) {
                return node.asText().trim();
            }
        }
        return "";
    }
}
