-- 카카오 사용자 가입 프로세스 완료 여부를 추적하기 위한 컬럼 추가
-- 기존 테이블 구조를 업데이트하여 가입 완료 상태를 관리

-- 1. registration_completed 컬럼 추가
ALTER TABLE tbl_user ADD COLUMN registration_completed bit NOT NULL DEFAULT false;

-- 2. 기존 일반 회원가입 사용자들은 가입 완료로 설정
UPDATE tbl_user SET registration_completed = true WHERE provider IS NULL OR provider = '';

-- 3. 기존 카카오 사용자들은 가입 미완료로 설정 (역할 설정을 하지 않았으므로)
UPDATE tbl_user SET registration_completed = false WHERE provider = 'kakao';

-- 4. 변경사항 확인
SELECT 
    email, 
    provider, 
    registration_completed,
    role
FROM tbl_user 
WHERE provider = 'kakao'
ORDER BY created_at DESC;
