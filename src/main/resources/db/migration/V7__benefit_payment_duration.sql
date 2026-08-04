-- 지급 기간 상한.
--
-- targetAgeMin/Max 는 "어떤 아이가 대상인가" 이지 "몇 개월 받는가" 가 아니다.
-- 이 둘을 같은 것으로 보면 육아휴직급여(월 150만원, 대상 0~96개월)가 60개월 전망에서
-- 9,000만원으로 계산된다. 실제로는 최대 12개월 지급이다.
ALTER TABLE TBL_POLICIES
    ADD COLUMN MAX_PAYMENT_MONTHS INT NULL COMMENT '월 지급 최대 개월 - NULL 이면 대상 연령 내내 지급';
