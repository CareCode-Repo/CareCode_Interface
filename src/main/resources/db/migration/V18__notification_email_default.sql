-- 이메일 알림 기본값을 끔으로 바꾼다.
-- 설정 행이 없을 때 실제로 발송되는 채널에는 이메일이 없는데 DDL 기본값만 TRUE 라,
-- 시드 스크립트나 수기 SQL 처럼 JPA 를 거치지 않는 삽입에서는 요청한 적 없는 이메일 알림이 켜진다.
-- 엔티티 기본값(false)과 맞춘다.

ALTER TABLE notification_preferences
    ALTER COLUMN EMAIL_ENABLED SET DEFAULT FALSE;
