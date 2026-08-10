package com.carecode.domain.admin.controller;

import com.carecode.core.devtools.SampleDataCleaner;
import com.carecode.core.devtools.SampleFacilitySeeder;
import com.carecode.core.devtools.SamplePolicySeeder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 샘플 데이터 관리. prod 프로파일에서는 빈 자체가 등록되지 않는다. */
@RestController
@RequestMapping("/api/admin/dev/sample-data")
@Profile("!prod")
@RequiredArgsConstructor
@Tag(name = "어드민 - 개발용 샘플 데이터", description = "공공데이터 연동 전 기능 확인용")
public class AdminSampleDataController {

    private final SamplePolicySeeder policySeeder;
    private final SampleFacilitySeeder facilitySeeder;
    private final SampleDataCleaner cleaner;

    @PostMapping
    @Operation(summary = "샘플 데이터 적재", description = "지역별 정책과 정원 관측 이력을 넣습니다")
    public ResponseEntity<Map<String, Integer>> seed() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("policies", policySeeder.seed());
        result.put("facilities", facilitySeeder.seed());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    @Operation(summary = "샘플 데이터 제거", description = "접두어로 식별해 실데이터는 남깁니다")
    public ResponseEntity<Map<String, Integer>> clean() {
        return ResponseEntity.ok(cleaner.clean());
    }
}
