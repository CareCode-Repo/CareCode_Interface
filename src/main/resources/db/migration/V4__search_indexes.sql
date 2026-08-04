-- V4: 위치 검색 및 전문 검색 인덱스
--
-- 반경 검색은 그동안 모든 행에 삼각함수를 계산한 뒤 HAVING 으로 걸러 풀 스캔이었다.
-- 바운딩 박스로 후보를 좁히도록 바꾸면서 BETWEEN 조건이 인덱스를 타도록 추가한다.
-- 검색은 LIKE '%키워드%' 라 인덱스를 쓸 수 없어 FULLTEXT 를 추가한다(ngram: 한글 대응).

-- 위치 검색: 위도로 범위를 좁히고 경도로 다시 좁힌다.
CREATE INDEX idx_facility_location ON TBL_CARE_FACILITIES (LATITUDE, LONGITUDE);
CREATE INDEX idx_hospital_location ON TBL_HOSPITAL (LATITUDE, LONGITUDE);

-- 전문 검색 (ngram 파서는 MySQL 5.7+ / MariaDB 10.0+ 에서 한글 토큰화에 필요)
CREATE FULLTEXT INDEX ft_facility_search ON TBL_CARE_FACILITIES (NAME, ADDRESS) WITH PARSER ngram;
CREATE FULLTEXT INDEX ft_policy_search ON TBL_POLICIES (TITLE, DESCRIPTION) WITH PARSER ngram;
CREATE FULLTEXT INDEX ft_post_search ON TBL_POST (TITLE, CONTENT) WITH PARSER ngram;

-- 목록 정렬에 쓰이는 컬럼
CREATE INDEX idx_facility_active_rating ON TBL_CARE_FACILITIES (IS_ACTIVE, RATING);
CREATE INDEX idx_policy_active_created ON TBL_POLICIES (IS_ACTIVE, CREATED_AT);
