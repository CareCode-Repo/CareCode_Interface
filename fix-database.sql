-- CareCode 데이터베이스 테이블 구조 수정 스크립트

-- 1. 기존 사용자 데이터 삭제
DELETE FROM TBL_USER;

-- 2. user_id 컬럼 제거 (만약 존재한다면)
-- MariaDB/MySQL에서는 컬럼이 존재하지 않으면 오류가 발생하므로 조건부로 실행
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
     WHERE TABLE_SCHEMA = 'carecode' 
     AND TABLE_NAME = 'TBL_USER' 
     AND COLUMN_NAME = 'user_id') > 0,
    'ALTER TABLE TBL_USER DROP COLUMN user_id',
    'SELECT "user_id column does not exist"'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. email 컬럼에 UNIQUE 제약 조건 추가 (만약 없다면)
ALTER TABLE TBL_USER ADD UNIQUE KEY uk_email (email);

-- 4. 테이블 구조 확인
DESCRIBE TBL_USER;

-- 5. 테스트 사용자 생성
INSERT INTO TBL_USER (
    email, 
    password, 
    name, 
    phone_number, 
    birth_date, 
    gender, 
    address, 
    latitude, 
    longitude, 
    role, 
    is_active, 
    email_verified, 
    created_at, 
    updated_at
) VALUES (
    'admin@carecode.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', -- admin123
    '관리자',
    '010-0000-0000',
    '1980-10-10',
    'MALE',
    '서울시 중구',
    37.5665,
    126.9780,
    'ADMIN',
    true,
    true,
    NOW(),
    NOW()
);

INSERT INTO TBL_USER (
    email, 
    password, 
    name, 
    phone_number, 
    birth_date, 
    gender, 
    address, 
    latitude, 
    longitude, 
    role, 
    is_active, 
    email_verified, 
    created_at, 
    updated_at
) VALUES (
    'test1@carecode.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', -- password123
    '테스트 사용자 1',
    '010-1234-5678',
    '1990-01-01',
    'FEMALE',
    '서울시 강남구',
    37.5665,
    126.9780,
    'PARENT',
    true,
    true,
    NOW(),
    NOW()
);

INSERT INTO TBL_USER (
    email, 
    password, 
    name, 
    phone_number, 
    birth_date, 
    gender, 
    address, 
    latitude, 
    longitude, 
    role, 
    is_active, 
    email_verified, 
    created_at, 
    updated_at
) VALUES (
    'test2@carecode.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', -- password123
    '테스트 사용자 2',
    '010-9876-5432',
    '1985-05-15',
    'MALE',
    '서울시 서초구',
    37.4837,
    127.0324,
    'PARENT',
    true,
    true,
    NOW(),
    NOW()
);

-- 6. 생성된 사용자 확인
SELECT id, email, name, role, is_active, email_verified FROM TBL_USER; 