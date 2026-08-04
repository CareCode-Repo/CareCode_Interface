package com.carecode.core.client.sync;

import com.carecode.core.client.provider.DataGoKrProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** 소아청소년과 병원 동기화. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PediatricHospitalSyncService {

    private static final int ROWS_PER_PAGE = 100;

    private final DataGoKrProvider provider;
    private final HospitalUpsertService upsertService;
    private final PagedSyncTemplate syncTemplate;

    @Value("${public.data.resource.hospital:B551182/hospInfoServicev2/getHospBasisList}")
    private String resource;

    /** 진료과목 코드. */
    @Value("${public.data.hospital.pediatric-subject-code:10}")
    private String pediatricSubjectCode;

    @Value("${public.data.hospital.subject-name:소아청소년과}")
    private String subjectName;

    public SyncResult sync() {
        return syncTemplate.run(SyncSpec.builder()
                .provider(provider)
                .resource(resource)
                .label("소아청소년과병원")
                .rowsPerPage(ROWS_PER_PAGE)
                .params(buildParams())
                .upsert(row -> upsertService.upsert(row, subjectName))
                .build());
    }

    private Map<String, String> buildParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("dgsbjtCd", pediatricSubjectCode);
        // 이 데이터셋은 XML 이 기본이다. JSON 을 지원하면 _type 으로 받고, 아니면 XML 로 파싱된다.
        params.put("_type", "json");
        return params;
    }
}
