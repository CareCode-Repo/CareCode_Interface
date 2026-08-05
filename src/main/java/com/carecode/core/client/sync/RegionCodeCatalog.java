package com.carecode.core.client.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 시군구 코드 목록. 지역 단위로만 조회되는 API 를 전국 수집할 때 순회 대상이 된다. */
@Slf4j
@Component
public class RegionCodeCatalog {

    private static final String KINDERGARTEN_REGIONS = "public-data/kindergarten-regions.txt";

    private final List<RegionCode> kindergartenRegions;

    public RegionCodeCatalog() {
        this.kindergartenRegions = load(KINDERGARTEN_REGIONS);
        log.info("유치원 조회 대상 시군구 {}개를 읽었습니다.", kindergartenRegions.size());
    }

    public record RegionCode(String sidoCode, String sggCode) {
    }

    public List<RegionCode> kindergartenRegions() {
        return kindergartenRegions;
    }

    /** 목록이 없으면 동기화가 조용히 0건으로 끝나므로 실패를 로그로 드러낸다. */
    private List<RegionCode> load(String path) {
        List<RegionCode> codes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split(",");
                if (parts.length == 2) {
                    codes.add(new RegionCode(parts[0].trim(), parts[1].trim()));
                }
            }
        } catch (Exception e) {
            log.error("시군구 코드 목록을 읽지 못했습니다: {}", path, e);
        }
        return Collections.unmodifiableList(codes);
    }
}
