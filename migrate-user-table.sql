-- 카카오 로그인 지원을 위한 tbl_user 테이블 마이그레이션
-- 기존 테이블 구조를 업데이트하여 OAuth 사용자를 지원

-- 1. password 컬럼을 nullable로 변경
ALTER TABLE tbl_user MODIFY COLUMN password varchar(255) NULL;

-- 2. provider 컬럼 추가 (OAuth 제공자: kakao, google, naver 등)
ALTER TABLE tbl_user ADD COLUMN provider varchar(255) NULL;

-- 3. provider_id 컬럼 추가 (OAuth 제공자의 사용자 ID)
ALTER TABLE tbl_user ADD COLUMN provider_id varchar(255) NULL;

-- 4. 기존 사용자들의 provider 정보 업데이트 (일반 회원가입 사용자)
UPDATE tbl_user SET provider = NULL, provider_id = NULL WHERE provider IS NULL;

-- 5. 인덱스 추가 (OAuth 사용자 조회 성능 향상)
CREATE INDEX idx_user_provider_provider_id ON tbl_user(provider, provider_id);
CREATE INDEX idx_user_email_provider ON tbl_user(email, provider);

-- 6. 변경사항 확인
SELECT 
    COLUMN_NAME, 
    IS_NULLABLE, 
    DATA_TYPE, 
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'tbl_user' 
AND TABLE_SCHEMA = 'carecode'
ORDER BY ORDINAL_POSITION;
