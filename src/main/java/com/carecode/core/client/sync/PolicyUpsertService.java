package com.carecode.core.client.sync;

import com.carecode.core.util.AgeRangeParser;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
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
        applyAgeRange(policy, row);
        policy.setUpdatedAt(LocalDateTime.now());

        policyRepository.save(policy);
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
