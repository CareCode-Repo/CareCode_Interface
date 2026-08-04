-- ================================================================================
-- V3: 병원 외부 식별자(심평원 암호화 요양기호) 추가
--
-- 공공데이터 동기화 시 같은 병원을 중복 적재하지 않기 위한 키다.
-- 심평원은 ykiho 를 암호화해 제공하며 복호화 수단이 없으므로 값 자체를 식별자로 쓴다.
-- 기존 수기 등록 병원은 NULL 로 남고, UNIQUE 제약은 NULL 을 중복으로 보지 않는다.
-- ================================================================================

ALTER TABLE TBL_HOSPITAL
    ADD COLUMN external_code VARCHAR(100) NULL COMMENT '심평원 암호화 요양기호(ykiho)';

ALTER TABLE TBL_HOSPITAL
    ADD CONSTRAINT uk_hospital_external_code UNIQUE (external_code);
