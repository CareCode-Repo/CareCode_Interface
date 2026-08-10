package com.carecode.core.client.sync;

import com.carecode.core.util.AgeRangeParser;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import com.carecode.domain.policy.service.PolicyChangeDetector;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 정책 한 건을 저장하는 트랜잭션 경계. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyUpsertService {

    /** 공공데이터 출처 정책임을 나타내는 코드 접두어. 관리자 수기 등록 정책과 구분한다. */
    public static final String EXTERNAL_CODE_PREFIX = "GOV-";

    private final PolicyRepository policyRepository;
    private final PolicyChangeDetector changeDetector;

    /** 서비스 ID 기준 upsert. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(cacheNames = "policy", allEntries = true)
    public boolean upsert(JsonNode row) {
        String serviceId = text(row, "서비스ID", "servId", "SVC_ID");
        if (serviceId == null) {
            throw new IllegalArgumentException("서비스 ID가 없는 응답입니다.");
        }

        String policyCode = EXTERNAL_CODE_PREFIX + serviceId;
        Policy policy = policyRepository.findByPolicyCode(policyCode).orElse(null);
        boolean isNew = policy == null;
        // 필드를 덮어쓰기 전에 값을 떠 둔다.
        PolicyChangeDetector.Before snapshot = isNew ? null : PolicyChangeDetector.Before.of(policy);
        if (isNew) {
            policy = new Policy();
            policy.setPolicyCode(policyCode);
            policy.setViewCount(0);
            policy.setIsActive(true);
        }

        policy.setTitle(text(row, "서비스명", "servNm", "SVC_NM"));
        policy.setDescription(text(row, "서비스목적요약", "servDgst", "SVC_DGST"));
        policy.setPolicyType(text(row, "서비스분야", "srvPvsnNm", "INTRS_THEMA_NM"));
        policy.setTargetRegion(resolveRegion(row));
        policy.setApplicationUrl(text(row, "상세조회URL", "servDtlLink", "DETAIL_URL"));
        policy.setContactInfo(text(row, "전화문의", "rprsCtadr", "CONTACT"));
        policy.setRequiredDocuments(text(row, "구비서류", "docCn"));
        // 지원유형("현금"/"이용권"/"서비스(일자리)")은 명확하지만 금액은 자유 텍스트라 자동 추출하지 않는다.
        // "국공립 100,000원, 사립 280,000원" 처럼 조건별로 갈리는 표기가 많아 틀린 금액이 확정치로 들어간다.
        policy.setBenefitType(text(row, "지원유형", "benefitType"));
        applyAgeRange(policy, row);
        policy.setUpdatedAt(LocalDateTime.now());

        // 저장하면 이전 값이 사라진다. 비교는 그 전에 끝내야 한다.
        PolicyChangeDetector.Before before = snapshot;
        Policy saved = policyRepository.save(policy);

        if (isNew) {
            changeDetector.recordCreated(saved);
        } else {
            changeDetector.recordUpdates(saved, before);
        }
        return isNew;
    }

    /**
     * 중앙행정기관 정책은 전국 공통이고, 지자체 정책은 소관기관명이 곧 지역이다.
     * 이 구분이 없으면 "교육부" 가 지역명으로 들어가 거주지 비교가 무의미해진다.
     */
    private String resolveRegion(JsonNode row) {
        String orgType = text(row, "소관기관유형", "orgTypeNm");
        String orgName = text(row, "소관기관명", "jurMnofNm", "JURISDICTION");

        if (orgType != null && orgType.contains("중앙")) {
            return "전국";
        }
        return orgName != null ? orgName : "전국";
    }

    /** 자유 텍스트에서 연령 조건을 개월로 환산해 채운다. 못 찾으면 기존 값을 건드리지 않는다. */
    private void applyAgeRange(Policy policy, JsonNode row) {
        String source = String.join(" ",
                nullToEmpty(text(row, "지원대상", "trgterIndvdlArray")),
                nullToEmpty(text(row, "선정기준", "slctCritCn")),
                nullToEmpty(text(row, "서비스목적요약", "servDgst", "SVC_DGST")));

        AgeRangeParser.AgeRange range = AgeRangeParser.parse(source);
        if (range == null) {
            return;
        }
        policy.setTargetAgeMin(range.minMonths());
        policy.setTargetAgeMax(range.maxMonths());
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
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
}
