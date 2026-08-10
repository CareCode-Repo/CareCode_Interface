-- 지급 기간 상한. targetAgeMin/Max 는 "어떤 아이가 대상인가" 이지 "몇 개월 받는가" 가 아니다
ALTER TABLE TBL_POLICIES
    ADD COLUMN MAX_PAYMENT_MONTHS INT NULL COMMENT '월 지급 최대 개월 - NULL 이면 대상 연령 내내 지급';
