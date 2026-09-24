package com.carecode.core.ops.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 주기 작업 실행 한 건. */
@Entity
@Table(name = "TBL_SYNC_RUN")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@lombok.AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SyncRun {

    /** 실행 결과. 실패와 "완료했지만 일부 실패" 를 구분해야 어디를 봐야 하는지 알 수 있다. */
    public enum Status {
        /** 끝까지 돌았고 실패 건이 없다. */
        SUCCESS,
        /** 끝까지 돌았지만 일부 항목이 실패했다. */
        PARTIAL,
        /** 중간에 멈췄다 (공공데이터 한도 초과 등). */
        INCOMPLETE,
        /** 예외로 죽었다. */
        FAILED;

        /** 신선도 판단에 쓸 수 있는 실행인가. 일부 실패는 데이터가 갱신됐으므로 인정한다. */
        public boolean countsAsFresh() {
            return this == SUCCESS || this == PARTIAL;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "JOB", nullable = false, length = 50)
    private String job;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private Status status;

    @Column(name = "STARTED_AT", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "FINISHED_AT", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "DURATION_MILLIS", nullable = false)
    private long durationMillis;

    @Column(name = "PROCESSED", nullable = false)
    private int processed;

    @Column(name = "FAILED", nullable = false)
    private int failed;

    @Column(name = "DETAIL", length = 1000)
    private String detail;
}
