-- 중복 수급 배타 그룹.
-- 부모급여와 양육수당은 동시에 받을 수 없는데 지금은 단순 합산돼 총액이 부풀려진다.
-- 같은 그룹에서는 금액이 가장 큰 것 하나만 계산해야 실제 수령액에 가까워진다.
ALTER TABLE TBL_POLICIES
    ADD COLUMN EXCLUSION_GROUP VARCHAR(60) NULL COMMENT '중복 수급 불가 그룹 - NULL 이면 다른 정책과 함께 받을 수 있음';

CREATE INDEX IDX_POLICIES_EXCLUSION ON TBL_POLICIES (EXCLUSION_GROUP, IS_ACTIVE);
