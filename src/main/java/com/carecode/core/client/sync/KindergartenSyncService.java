package com.carecode.core.client.sync;

import com.carecode.core.client.provider.DataGoKrProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 전국 유치원 표준데이터 동기화. 어린이집만 있고 유치원이 비어 있던 공백을 메운다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class KindergartenSyncService {

    private static final int ROWS_PER_PAGE = 500;

    private final DataGoKrProvider provider;
    private final KindergartenUpsertService upsertService;
    private final PagedSyncTemplate syncTemplate;

    /** 표준데이터는 apis.data.go.kr 이 아닌 별도 호스트라 절대 URL 로 지정한다. */
    @Value("${public.data.resource.kindergarten:"
            + "http://api.data.go.kr/openapi/tn_pubr_public_kindergarten_api}")
    private String resource;

    public SyncResult sync() {
        return syncTemplate.run(SyncSpec.builder()
                .provider(provider)
                .resource(resource)
                .label("전국유치원")
                .rowsPerPage(ROWS_PER_PAGE)
                .upsert(upsertService::upsert)
                .build());
    }
}
