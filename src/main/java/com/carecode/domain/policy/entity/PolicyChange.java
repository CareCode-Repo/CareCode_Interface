package com.carecode.domain.policy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 정책이 어떻게 바뀌었는지. 값이 덮어써지기 전에 남겨야 알림을 만들 수 있다. */
@Entity
@Table(name = "TBL_POLICY_CHANGE")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PolicyChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "POLICY_ID", nullable = false)
    private Long policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANGE_TYPE", nullable = false, length = 30)
    private ChangeType changeType;

    @Column(name = "FIELD_NAME", length = 50)
    private String fieldName;

    @Column(name = "OLD_VALUE", length = 500)
    private String oldValue;

    @Column(name = "NEW_VALUE", length = 500)
    private String newValue;

    /** 알림 대상을 고를 때 정책을 다시 조회하지 않도록 중복 저장한다. */
    @Column(name = "TARGET_REGION", length = 200)
    private String targetRegion;

    @Column(name = "DETECTED_AT", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "NOTIFIED", nullable = false)
    @Builder.Default
    private Boolean notified = false;

    /** 사용자에게 알릴 가치가 있는 변경만 정의한다. 오탈자 수정까지 알리면 알림이 소음이 된다. */
    public enum ChangeType {
        CREATED("신규 지원금"),
        AMOUNT_CHANGED("지원금액 변경"),
        DEADLINE_CHANGED("신청기한 변경"),
        AGE_RANGE_CHANGED("대상 연령 변경");

        private final String displayName;

        ChangeType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }
    }

    public void markNotified() {
        this.notified = true;
    }
}
