package com.carecode.domain.user.service;

import com.carecode.core.exception.ConsentRequiredException;
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
            throw new ConsentRequiredException(type.name(), type.getDisplayName(), type.isSensitive());
        }
    }

    public boolean hasConsent(Long userId, ConsentType type) {
        return consentRepository.findLatest(userId, type)
                .map(UserConsent::isGranted)
                .orElse(false);
    }
}
