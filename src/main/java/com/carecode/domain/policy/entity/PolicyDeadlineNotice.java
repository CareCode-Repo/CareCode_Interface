package com.carecode.domain.policy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 마감 임박 알림을 누구에게 언제 보냈는지.
 *
 * <p>보낸 사실을 남겨두지 않으면 스케줄러가 하루에 두 번 돌거나 인스턴스가 두 대일 때
 * 같은 알림이 반복해서 나간다. 지원금 알림은 한 번 더 오는 순간 신뢰를 잃는다.
 */
@Entity
@Table(name = "TBL_POLICY_DEADLINE_NOTICE",
        uniqueConstraints = @UniqueConstraint(name = "UK_POLICY_DEADLINE_NOTICE",
                columnNames = {"POLICY_ID", "USER_ID", "NOTIFIED_ON"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDeadlineNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "POLICY_ID", nullable = false)
    private Long policyId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "NOTIFIED_ON", nullable = false)
    private LocalDate notifiedOn;

    /** 발송 시점의 잔여 일수. D-7 과 D-1 중 어느 쪽이 실제로 열렸는지 보려면 필요하다. */
    @Column(name = "DAYS_LEFT", nullable = false)
    private Integer daysLeft;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
