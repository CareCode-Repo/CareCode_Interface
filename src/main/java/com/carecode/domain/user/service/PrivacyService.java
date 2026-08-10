package com.carecode.domain.user.service;

import com.carecode.core.security.CurrentUserFacade;
import com.carecode.domain.community.repository.PostRepository;
import com.carecode.domain.health.repository.HealthRecordRepository;
import com.carecode.domain.user.dto.request.ConsentUpdateRequest;
import com.carecode.domain.user.dto.response.ConsentStatusResponse;
import com.carecode.domain.user.entity.ConsentType;
import com.carecode.domain.user.entity.User;
import com.carecode.domain.user.entity.UserConsent;
import com.carecode.domain.user.repository.ChildRepository;
import com.carecode.domain.user.repository.UserConsentRepository;
import com.carecode.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 개인정보 관련 기능: 동의 이력 관리, 내 데이터 열람, 파기. 개인정보보호법상 정보주체는 자신의 정보를 열람하고 처리 정지·삭제를 요구할 수 있다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrivacyService {

    private final UserConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final PostRepository postRepository;
    private final CurrentUserFacade currentUserFacade;

    // ====================
    // 동의 관리 ====================

    /** 동의 상태를 기록한다. 기존 행을 수정하지 않고 새 이력을 남긴다. 언제 무엇에 동의/철회했는지 추적해야 하기 때문이다. */
    @Transactional
    public ConsentStatusResponse recordConsent(ConsentUpdateRequest request, String ipAddress) {
        User user = currentUserFacade.requireCurrentUser();

        if (request.getConsentType().isRequired() && !request.isGranted()) {
            log.info("필수 항목 철회 요청 - userId={}, type={}", user.getId(), request.getConsentType());
        }

        UserConsent consent = UserConsent.builder()
                .user(user)
                .consentType(request.getConsentType())
                .policyVersion(request.getPolicyVersion())
                .granted(request.isGranted())
                .ipAddress(ipAddress)
                .build();

        consentRepository.save(consent);
        return getConsentStatus();
    }

    /** 항목별 현재 동의 상태. */
    public ConsentStatusResponse getConsentStatus() {
        User user = currentUserFacade.requireCurrentUser();

        List<ConsentStatusResponse.ConsentItem> items = new ArrayList<>();
        for (ConsentType type : ConsentType.values()) {
            UserConsent latest = consentRepository.findLatest(user.getId(), type).orElse(null);
            items.add(ConsentStatusResponse.ConsentItem.builder()
                    .consentType(type.name())
                    .displayName(type.getDisplayName())
                    .required(type.isRequired())
                    .granted(latest != null && latest.isGranted())
                    .policyVersion(latest != null ? latest.getPolicyVersion() : null)
                    .updatedAt(latest != null ? latest.getCreatedAt() : null)
                    .build());
        }

        return ConsentStatusResponse.builder().consents(items).build();
    }

    public List<ConsentStatusResponse.ConsentHistoryItem> getConsentHistory() {
        User user = currentUserFacade.requireCurrentUser();
        return consentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(c -> ConsentStatusResponse.ConsentHistoryItem.builder()
                        .consentType(c.getConsentType().name())
                        .displayName(c.getConsentType().getDisplayName())
                        .granted(c.isGranted())
                        .policyVersion(c.getPolicyVersion())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    // ====================
    // 데이터 열람 ====================

    /** 내 데이터 전체 내려받기. 정보주체의 열람권 행사에 대응한다. 비밀번호 등 인증 정보는 포함하지 않는다. */
    public Map<String, Object> exportMyData() {
        User user = currentUserFacade.requireCurrentUser();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user.getUserId());
        profile.put("email", user.getEmail());
        profile.put("name", user.getName());
        profile.put("phoneNumber", user.getPhoneNumber());
        profile.put("birthDate", user.getBirthDate());
        profile.put("address", user.getAddress());
        profile.put("role", user.getRole() != null ? user.getRole().name() : null);
        profile.put("createdAt", user.getCreatedAt());
        profile.put("lastLoginAt", user.getLastLoginAt());

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", LocalDateTime.now());
        export.put("profile", profile);
        export.put("children", childRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(child -> Map.of(
                        "name", child.getName(),
                        "birthDate", String.valueOf(child.getBirthDate()),
                        "gender", String.valueOf(child.getGender())))
                .toList());
        export.put("healthRecordCount", healthRecordRepository.findByUserOrderByRecordDateDesc(user).size());
        export.put("postCount", postRepository.countByAuthorId(user.getId()));
        export.put("consentHistory", getConsentHistory());

        return export;
    }

    // ====================
    // 파기 ====================

    /** 회원 탈퇴(파기 요청). 즉시 물리 삭제하지 않는 이유: 게시글·댓글 등 참조 데이터가 함께 사라지면 다른 이용자의 대화 맥락이 깨지고 */
    @Transactional
    public void deleteMyAccount() {
        User user = currentUserFacade.requireCurrentUser();

        String anonymized = "deleted_" + user.getId();
        user.setName("탈퇴한 사용자");
        user.setEmail(anonymized + "@deleted.local");
        user.setPassword(null);
        user.setPhoneNumber(null);
        user.setAddress(null);
        user.setProfileImageUrl(null);
        user.setProviderId(null);
        user.setLatitude(null);
        user.setLongitude(null);
        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("회원 탈퇴 처리 완료 - userId={}", user.getId());
    }
}
