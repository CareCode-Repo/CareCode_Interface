package com.carecode.domain.policy.service;

import com.carecode.core.analytics.EventLogger;
import com.carecode.core.analytics.EventType;
import com.carecode.core.exception.ResourceNotFoundException;
import com.carecode.domain.policy.entity.Policy;
import com.carecode.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지원금 신청 링크. 클릭을 집계한 뒤 실제 신청처 주소를 돌려준다.
 *
 * <p>이 전환율이 서비스가 실제로 돈을 찾아줬는지 증명하는 유일한 지표다. 그래서 프런트는
 * 정책의 주소로 바로 가지 않고 항상 이 경로를 거친다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BenefitLinkService {

    private final PolicyRepository policyRepository;
    private final EventLogger eventLogger;

    /**
     * 클릭을 집계하고 이동할 주소를 돌려준다.
     *
     * @param actorUserId 비로그인 클릭도 집계 대상이라 {@code null} 일 수 있다
     * @throws ResourceNotFoundException 정책이 없거나 온라인 신청 경로가 없을 때
     */
    public String resolveApplyUrl(Long policyId, Long actorUserId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("정책을 찾을 수 없습니다: " + policyId));

        String url = policy.getApplicationUrl();
        // 온라인 신청 경로가 없는 정책은 정상적인 데이터 상태다. 전에는 CareServiceException
        // 이라 500 이 나갔고, 그러면 모니터링에 서버 장애로 집계된다.
        // (프런트는 주소가 없으면 버튼을 띄우지 않으므로 앱 정상 흐름에서는 닿지 않는다.)
        if (url == null || url.isBlank()) {
            throw new ResourceNotFoundException("이 정책은 온라인 신청 경로가 없습니다: " + policyId);
        }
        // 외부 주소로 보내므로 스킴을 확인한다. javascript: 같은 값이 들어오면 XSS 가 된다.
        // 이쪽은 우리 DB 에 잘못된 값이 들어온 경우라 서버 오류로 남기는 것이 맞다.
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalStateException("정책의 신청 링크 스킴이 허용되지 않습니다: policyId=" + policyId);
        }

        eventLogger.log(EventType.BENEFIT_LINK_CLICKED, actorUserId, String.valueOf(policyId));
        return url;
    }
}
