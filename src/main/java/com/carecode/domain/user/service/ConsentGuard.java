package com.carecode.domain.user.service;

import com.carecode.core.exception.CareServiceException;
import com.carecode.domain.user.entity.ConsentType;
import com.carecode.domain.user.entity.UserConsent;
import com.carecode.domain.user.repository.UserConsentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 민감정보 동의 확인. 동의를 받아두기만 하고 강제하지 않으면 받지 않은 것과 같다. */
@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentGuard {

    private final UserConsentRepository consentRepository;

    /** 동의가 없거나 철회됐으면 접근을 막는다. */
    public void require(Long userId, ConsentType type) {
        if (!hasConsent(userId, type)) {
            throw new ConsentRequiredException(type);
        }
    }

    public boolean hasConsent(Long userId, ConsentType type) {
        return consentRepository.findLatest(userId, type)
                .map(UserConsent::isGranted)
                .orElse(false);
    }

    /** 동의가 필요해서 막힌 것인지 클라이언트가 구분할 수 있게 별도 예외로 던진다. */
    public static class ConsentRequiredException extends CareServiceException {

        private final transient ConsentType consentType;

        public ConsentRequiredException(ConsentType consentType) {
            super(consentType.getDisplayName() + " 동의가 필요합니다.");
            this.consentType = consentType;
        }

        public ConsentType getConsentType() {
            return consentType;
        }
    }
}
