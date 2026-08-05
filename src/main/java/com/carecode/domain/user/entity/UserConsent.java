package com.carecode.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 동의 이력. 개인정보보호법상 "언제, 어떤 버전의 약관에, 무엇을 동의했는지" 를 입증할 수 있어야 한다 */
@Entity
@Table(
    name = "TBL_USER_CONSENT",
    indexes = {
        @Index(name = "idx_consent_user", columnList = "user_id"),
        @Index(name = "idx_consent_user_type", columnList = "user_id,consent_type")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 40)
    private ConsentType consentType;

    /** 동의한 약관 버전. 약관이 개정되면 재동의를 받아야 한다. */
    @Column(name = "policy_version", nullable = false, length = 20)
    private String policyVersion;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    /** 동의 시점의 접속 IP. 분쟁 시 입증 자료로 쓰인다. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
