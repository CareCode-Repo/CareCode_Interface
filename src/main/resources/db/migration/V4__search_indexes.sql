-- V4: 위치 검색 및 전문 검색 인덱스. 반경 검색은 그동안 모든 행에 삼각함수를 계산해 풀 스캔이었다

-- 위치 검색: 위도로 범위를 좁히고 경도로 다시 좁힌다.
CREATE INDEX idx_facility_location ON TBL_CARE_FACILITIES (LATITUDE, LONGITUDE);
CREATE INDEX idx_hospital_location ON TBL_HOSPITAL (LATITUDE, LONGITUDE);

-- 전문 검색.
-- ngram 파서는 MySQL 전용이라 MariaDB 에서는 "Function 'ngram' is not defined" 로 기동이 실패한다.
-- MariaDB 내장 토크나이저는 공백 단위라 "행복어린이집" 같은 붙은 말은 부분 일치가 안 되는데,
-- FullTextSearchSupport 가 짧은 키워드·미지원 상황을 LIKE 로 폴백하므로 검색 자체는 동작한다.
CREATE FULLTEXT INDEX ft_facility_search ON TBL_CARE_FACILITIES (NAME, ADDRESS);
CREATE FULLTEXT INDEX ft_policy_search ON TBL_POLICIES (TITLE, DESCRIPTION);
CREATE FULLTEXT INDEX ft_post_search ON TBL_POST (TITLE, CONTENT);

-- 목록 정렬에 쓰이는 컬럼
CREATE INDEX idx_facility_active_rating ON TBL_CARE_FACILITIES (IS_ACTIVE, RATING);
CREATE INDEX idx_policy_active_created ON TBL_POLICIES (IS_ACTIVE, CREATED_AT);
