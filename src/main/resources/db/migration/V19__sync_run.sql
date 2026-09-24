-- 주기 작업 실행 이력.
--
-- 지금까지 스케줄 작업은 로그만 남겼다. 로그는 지나가면 사라지고 질의할 수 없어서
-- "마지막으로 성공한 게 언제인가" 를 아무도 답할 수 없었다. 동기화가 조용히 멈추면
-- 데이터는 낡아가는데 화면은 그대로 보여준다 - 이 프로젝트에서 반복된 실패 방식이다.
--
-- 이력을 남겨 (1) 신선도 지표/알림의 근거로 쓰고, (2) 공개 통계에 기준 시각을 함께 준다.

CREATE TABLE TBL_SYNC_RUN (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    JOB VARCHAR(50) NOT NULL COMMENT '작업 코드 (SyncJob enum)',
    STATUS VARCHAR(20) NOT NULL COMMENT 'SUCCESS, PARTIAL, INCOMPLETE, FAILED',
    STARTED_AT DATETIME NOT NULL,
    FINISHED_AT DATETIME NOT NULL,
    DURATION_MILLIS BIGINT NOT NULL,
    PROCESSED INT NOT NULL DEFAULT 0 COMMENT '생성+수정 건수',
    FAILED INT NOT NULL DEFAULT 0,
    DETAIL VARCHAR(1000) NULL COMMENT '중단 사유나 예외 요약',

    -- 신선도 조회는 "작업별 최근 성공 1건" 이라 이 순서가 필요하다.
    INDEX IDX_SYNC_RUN_JOB_STATUS_FINISHED (JOB, STATUS, FINISHED_AT),
    INDEX IDX_SYNC_RUN_FINISHED (FINISHED_AT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='주기 작업 실행 이력';
