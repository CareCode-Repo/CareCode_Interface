package com.carecode.core.client.sync;

import com.carecode.core.client.provider.ChildcarePortalProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 전국 어린이집 정보 동기화. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NationwideChildcareFacilitySyncService {

    private static final int ROWS_PER_PAGE = 500;

    private final ChildcarePortalProvider provider;
    private final CareFacilityUpsertService upsertService;
    private final PagedSyncTemplate syncTemplate;

    /** 데이터셋 경로는 개편될 수 있어 재배포 없이 바꿀 수 있게 프로퍼티로 둔다. */
    @Value("${public.data.resource.childcare:http://api.childcare.go.kr/mediate/rest/cpmsapi021/cpmsapi021/request}")
    private String resource;

    public SyncResult sync() {
        return syncTemplate.run(SyncSpec.builder()
                .provider(provider)
                .resource(resource)
                .label("전국어린이집")
                .rowsPerPage(ROWS_PER_PAGE)
                .upsert(upsertService::upsert)
                .build());
    }
}
