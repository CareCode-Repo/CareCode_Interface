package com.carecode.domain.policy.entity;

import com.carecode.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 실제로 받아본 사람이 알려주는 금액. 공공데이터가 채우지 못하는 공백을 메운다. */
@Entity
@Table(name = "TBL_BENEFIT_AMOUNT_REPORT",
        uniqueConstraints = @UniqueConstraint(name = "UK_BENEFIT_REPORT_USER",
                columnNames = {"POLICY_ID", "USER_ID"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BenefitAmountReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "POLICY_ID", nullable = false)
    private Long policyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    @Column(name = "REPORTED_AMOUNT", nullable = false)
    private Integer reportedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TYPE", nullable = false, length = 20)
    private PaymentType paymentType;

    @Column(name = "RECEIVED_AT")
    private LocalDate receivedAt;

    @Column(name = "NOTE", length = 300)
    private String note;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    /** 월 지급인지 1회인지에 따라 같은 금액이라도 연간 총액이 12배 차이 난다. */
    public enum PaymentType {
        MONTHLY, ONE_TIME
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void update(Integer amount, PaymentType type, LocalDate receivedAt, String note) {
        this.reportedAmount = amount;
        this.paymentType = type;
        this.receivedAt = receivedAt;
        this.note = note;
    }
}
