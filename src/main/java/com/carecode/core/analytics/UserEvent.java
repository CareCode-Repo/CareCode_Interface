package com.carecode.core.analytics;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 한 번 쓰면 고치지 않는 append-only 기록. 지표를 나중에 다시 계산할 수 있어야 한다. */
@Entity
@Table(name = "TBL_USER_EVENT")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    /** 비로그인 이벤트는 null. */
    @Column(name = "USER_ID")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 60)
    private EventType eventType;

    @Column(name = "TARGET_ID", length = 100)
    private String targetId;

    @Column(name = "METADATA", length = 500)
    private String metadata;

    @Column(name = "OCCURRED_AT", nullable = false)
    private LocalDateTime occurredAt;

    /** 집계 쿼리가 인덱스를 타도록 날짜를 따로 저장한다. */
    @Column(name = "OCCURRED_DATE", nullable = false)
    private LocalDate occurredDate;

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
        occurredDate = occurredAt.toLocalDate();
    }
}
