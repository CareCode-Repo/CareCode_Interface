-- ================================================================================
-- V2: 예방접종 일정, 커뮤니티 모더레이션, 개인정보 동의 이력
--
-- prod 는 ddl-auto=validate 이므로 엔티티를 추가할 때마다 이 파일 같은
-- 포워드 마이그레이션을 함께 넣어야 기동된다.
-- ================================================================================

-- --------------------------------------------------------------------------------
-- 1. 예방접종 일정 (아이 등록 시 표준 일정이 자동 생성됨)
-- --------------------------------------------------------------------------------
CREATE TABLE TBL_VACCINATION_SCHEDULE (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    child_id         BIGINT NOT NULL,
    vaccine_type     VARCHAR(50) NOT NULL COMMENT '백신 종류 (BCG, DTAP, MMR 등)',
    dose_number      INT NOT NULL COMMENT '접종 회차 (1부터)',
    due_date         DATE NOT NULL COMMENT '표준 일정상 접종 예정일',
    completed_date   DATE COMMENT '실제 접종일',
    status           VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' COMMENT 'SCHEDULED, COMPLETED, SKIPPED',
    reminder_sent_at DATETIME COMMENT '사전 알림 발송 시각 (중복 발송 방지)',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_vaccination_child FOREIGN KEY (child_id) REFERENCES TBL_CHILD(ID) ON DELETE CASCADE,
    CONSTRAINT uk_vaccination_child_vaccine_dose UNIQUE (child_id, vaccine_type, dose_number),
    INDEX idx_vaccination_child (child_id),
    INDEX idx_vaccination_due_date (due_date),
    INDEX idx_vaccination_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='아이별 예방접종 일정';

-- --------------------------------------------------------------------------------
-- 2. 게시글·댓글 신고
-- --------------------------------------------------------------------------------
CREATE TABLE TBL_REPORT (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id    BIGINT NOT NULL,
    target_type    VARCHAR(20) NOT NULL COMMENT 'POST, COMMENT',
    target_id      BIGINT NOT NULL,
    reason         VARCHAR(30) NOT NULL COMMENT 'SPAM, ABUSE, SEXUAL, PRIVACY, FALSE_INFO, OTHER',
    detail         VARCHAR(1000),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, ACCEPTED, REJECTED',
    moderator_note VARCHAR(1000) COMMENT '관리자 처리 메모',
    resolved_at    DATETIME,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_report_reporter FOREIGN KEY (reporter_id) REFERENCES TBL_USER(ID) ON DELETE CASCADE,
    CONSTRAINT uk_report_reporter_target UNIQUE (reporter_id, target_type, target_id),
    INDEX idx_report_status (status),
    INDEX idx_report_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='커뮤니티 신고';

-- --------------------------------------------------------------------------------
-- 3. 사용자 차단
-- --------------------------------------------------------------------------------
CREATE TABLE TBL_USER_BLOCK (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    blocker_id BIGINT NOT NULL COMMENT '차단한 사용자',
    blocked_id BIGINT NOT NULL COMMENT '차단당한 사용자',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_block_blocker FOREIGN KEY (blocker_id) REFERENCES TBL_USER(ID) ON DELETE CASCADE,
    CONSTRAINT fk_user_block_blocked FOREIGN KEY (blocked_id) REFERENCES TBL_USER(ID) ON DELETE CASCADE,
    CONSTRAINT uk_user_block UNIQUE (blocker_id, blocked_id),
    INDEX idx_user_block_blocker (blocker_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='사용자 차단';

-- --------------------------------------------------------------------------------
-- 4. 개인정보 동의 이력 (append-only)
--
-- 현재 상태를 덮어쓰지 않고 동의·철회를 각각 새 행으로 남긴다.
-- "언제, 어떤 버전 약관에, 무엇을 동의했는지" 를 입증할 수 있어야 하기 때문이다.
-- --------------------------------------------------------------------------------
CREATE TABLE TBL_USER_CONSENT (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    consent_type   VARCHAR(40) NOT NULL COMMENT 'TERMS_OF_SERVICE, PRIVACY_POLICY, CHILD_DATA, MARKETING, THIRD_PARTY_SHARING',
    policy_version VARCHAR(20) NOT NULL COMMENT '동의한 약관 버전',
    granted        BOOLEAN NOT NULL COMMENT '동의 여부 (철회 시 false)',
    ip_address     VARCHAR(45) COMMENT '동의 시점 접속 IP (분쟁 시 입증 자료)',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES TBL_USER(ID) ON DELETE CASCADE,
    INDEX idx_consent_user (user_id),
    INDEX idx_consent_user_type (user_id, consent_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='개인정보 동의 이력';
