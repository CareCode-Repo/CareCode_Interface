package com.carecode.core.client.sync;

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

/**
 * 정책 한 건을 저장하는 트랜잭션 경계.
 *
 * <p>관리자가 직접 수정한 정책을 공공데이터가 덮어쓰지 않도록,
 * 외부에서 받은 정책은 {@code policyCode} 에 접두어를 붙여 출처를 구분한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyUpsertService {

    /** 공공데이터 출처 정책임을 나타내는 코드 접두어. 관리자 수기 등록 정책과 구분한다. */
    public static final String EXTERNAL_CODE_PREFIX = "GOV-";

    private final PolicyRepository policyRepository;

    /**
     * 서비스 ID 기준 upsert.
     *
     * @return 신규 생성이면 true
     */
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
        policy.setTargetRegion(text(row, "소관기관명", "jurMnofNm", "JURISDICTION"));
        policy.setApplicationUrl(text(row, "상세조회URL", "servDtlLink", "DETAIL_URL"));
        policy.setContactInfo(text(row, "전화문의", "rprsCtadr", "CONTACT"));
        policy.setRequiredDocuments(text(row, "구비서류", "docCn"));
        policy.setUpdatedAt(LocalDateTime.now());

        policyRepository.save(policy);
        return isNew;
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
