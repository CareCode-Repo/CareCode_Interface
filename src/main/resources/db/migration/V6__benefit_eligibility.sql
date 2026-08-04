-- 놓친 지원금 발굴에 필요한 자격 조건.
-- 연령·지역만으로는 실제 수급 가능 여부를 가릴 수 없어 소득·다자녀·소급 조건을 추가한다.

-- 기준중위소득 대비 % 이하가 대상. NULL 이면 소득 무관 정책이다.
ALTER TABLE TBL_POLICIES
    ADD COLUMN INCOME_THRESHOLD_PERCENT INT NULL COMMENT '기준중위소득 대비 상한(%) - NULL 이면 소득 무관';

-- 자녀 수 요건. NULL 이면 무관, 2 면 다자녀 정책.
ALTER TABLE TBL_POLICIES
    ADD COLUMN MIN_CHILDREN INT NULL COMMENT '최소 자녀 수 요건 - NULL 이면 무관';

-- 대상 연령이 지난 뒤에도 신청 가능한 기간(개월). NULL 이면 소급 불가.
ALTER TABLE TBL_POLICIES
    ADD COLUMN RETROACTIVE_MONTHS INT NULL COMMENT '소급 신청 가능 개월 - NULL 이면 소급 불가';

-- 사용자 가구 소득. 소득 조건이 붙은 정책을 거르는 데 쓴다.
-- 실제 금액이 아니라 기준중위소득 대비 비율만 저장한다 (민감정보 최소 수집).
ALTER TABLE TBL_USER
    ADD COLUMN INCOME_PERCENT INT NULL COMMENT '가구 소득 / 기준중위소득 (%) - NULL 이면 미입력';

ALTER TABLE TBL_USER
    ADD COLUMN HOUSEHOLD_SIZE INT NULL COMMENT '가구원 수';

-- 소급 판정은 "소급 가능한 정책" 만 훑으므로 부분 인덱스 대신 조건 컬럼에 인덱스를 둔다.
CREATE INDEX IDX_POLICIES_RETROACTIVE ON TBL_POLICIES (RETROACTIVE_MONTHS, IS_ACTIVE);
