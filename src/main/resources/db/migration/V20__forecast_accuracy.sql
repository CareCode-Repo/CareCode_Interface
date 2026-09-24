-- 입소 예측 정확도 측정 결과.
--
-- 지금까지 "입소 확률 40%" 를 보여주면서 그 숫자가 맞았는지 확인한 적이 없다. 정원 관측 시계열이
-- 이미 쌓이고 있으므로, 과거 시점으로 돌아가 그때 관측만으로 예측을 다시 계산하고 그 뒤 실제로
-- 자리가 났는지 맞춰볼 수 있다(백테스트). 사용자에게는 확률과 함께 이 적중률을 같이 보여준다.

CREATE TABLE TBL_FORECAST_ACCURACY (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    RUN_DATE DATE NOT NULL COMMENT '측정 실행일',
    HORIZON_MONTHS INT NOT NULL COMMENT '예측 기간(개월)',
    SAMPLES INT NOT NULL COMMENT '검증에 쓴 예측 건수',
    FACILITIES INT NOT NULL COMMENT '표본에 포함된 시설 수',
    BRIER_SCORE DOUBLE NOT NULL COMMENT '낮을수록 정확 (0=완벽, 0.25=동전 던지기)',
    BASELINE_BRIER_SCORE DOUBLE NOT NULL COMMENT '항상 평균 발생률로 답했을 때의 점수. 이보다 낮아야 예측이 의미 있다',
    ACTUAL_RATE DOUBLE NOT NULL COMMENT '표본에서 실제로 자리가 난 비율',

    -- 확률대별 적중률. 화면 표시에만 쓰고 질의 대상이 아니라 JSON 으로 둔다.
    -- 구간 정의가 바뀔 수 있어 칼럼으로 고정하지 않는다.
    CALIBRATION_JSON TEXT NOT NULL COMMENT '[{from,to,samples,actualTrue}] 형태',
    CREATED_AT DATETIME NOT NULL,

    -- 조회는 "기간별 최신 1건" 이다.
    INDEX IDX_FORECAST_ACCURACY_HORIZON_RUN (HORIZON_MONTHS, RUN_DATE)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='입소 예측 정확도 측정 결과';
