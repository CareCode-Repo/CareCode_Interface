-- 요양기관 종별. type 에는 진료과목(소아청소년과)이 들어가야 검색이 되므로 종별은 따로 둔다
-- "의원"(동네 소아과)과 "상급종합"(대학병원)은 부모의 선택 기준이 완전히 다르다
ALTER TABLE TBL_HOSPITAL
    ADD COLUMN GRADE VARCHAR(50) NULL COMMENT '요양기관 종별 (의원/병원/종합병원/상급종합)';

CREATE INDEX IDX_HOSPITAL_TYPE_GRADE ON TBL_HOSPITAL (TYPE, GRADE);
