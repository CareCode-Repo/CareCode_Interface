-- 카카오 사용자 확인
SELECT * FROM TBL_USER 
WHERE email = 'kakao_440173530@kakao.com' 
   OR provider_id = '440173530'
   OR user_id = 'user_1755468784198_788';

-- 모든 카카오 사용자 확인  
SELECT user_id, email, name, provider, provider_id, created_at 
FROM TBL_USER 
WHERE provider = 'kakao';
