-- 테스트용 카카오 사용자 삭제
DELETE FROM TBL_USER WHERE email = 'kakao_440173530@kakao.com';
DELETE FROM TBL_USER WHERE provider_id = '440173530';
