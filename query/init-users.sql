-- CareCode 테스트 사용자 생성 스크립트
-- MariaDB/MySQL용

-- 기존 테스트 사용자 삭제 (선택사항)
DELETE FROM TBL_USER WHERE email IN ('admin@carecode.com', 'test1@carecode.com', 'test2@carecode.com');

-- 관리자 사용자 생성
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

-- 테스트 사용자 1 생성
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

-- 테스트 사용자 2 생성
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

-- 생성된 사용자 확인
SELECT id, email, name, role, is_active, email_verified FROM TBL_USER WHERE email IN ('admin@carecode.com', 'test1@carecode.com', 'test2@carecode.com'); 