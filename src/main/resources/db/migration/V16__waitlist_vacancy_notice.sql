-- 빈자리 알림 발송 이력.
-- 정원 스냅샷은 자리가 났다는 사실을 알고 있었고 대기 명단도 있었는데 둘이 이어져 있지 않아
-- 지금까지는 사용자가 직접 들어와야만 알 수 있었다. 이 컬럼이 중복 발송을 막는 기준이 된다.

ALTER TABLE TBL_FACILITY_WAITLIST
    ADD COLUMN VACANCY_NOTIFIED_AT DATE NULL COMMENT '마지막으로 빈자리를 알린 관측일';
