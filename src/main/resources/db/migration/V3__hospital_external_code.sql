-- ================================================================================ V3: 병원 외부 식별자(심평원 암

ALTER TABLE TBL_HOSPITAL
    ADD COLUMN external_code VARCHAR(100) NULL COMMENT '심평원 암호화 요양기호(ykiho)';

ALTER TABLE TBL_HOSPITAL
    ADD CONSTRAINT uk_hospital_external_code UNIQUE (external_code);
