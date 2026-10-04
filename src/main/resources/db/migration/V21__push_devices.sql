-- 사용자당 푸시 기기 여러 대.
--
-- 지금까지 토큰은 TBL_NOTIFICATION_PREFERENCE 의 한 칸(DEVICE_TOKEN)에 들어 있었다.
-- 그래서 웹에서 알림을 켜 둔 사람이 앱에서 다시 켜면 앞의 토큰을 덮어썼고, 한쪽은 조용히
-- 알림을 받지 못했다. 토큰은 "사용자의 성질" 이 아니라 "기기의 성질" 이라 행을 따로 둔다.
CREATE TABLE TBL_PUSH_DEVICE (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    USER_ID BIGINT NOT NULL,
    -- FCM 등록 토큰. 기기를 지우고 다시 깔면 새 토큰이 나온다.
    TOKEN VARCHAR(512) NOT NULL,
    -- WEB / ANDROID / IOS. 발송 경로를 가르지는 않지만, "알림이 안 와요" 를 추적하려면 필요하다.
    DEVICE_TYPE VARCHAR(20) NULL,
    APP_VERSION VARCHAR(40) NULL,
    CREATED_AT DATETIME NOT NULL,
    UPDATED_AT DATETIME NOT NULL,
    -- 같은 토큰이 두 사용자에게 달릴 수 없다. 기기를 바꿔 로그인하면 소유자만 옮겨 간다.
    CONSTRAINT UK_PUSH_DEVICE_TOKEN UNIQUE (TOKEN),
    CONSTRAINT FK_PUSH_DEVICE_USER FOREIGN KEY (USER_ID) REFERENCES TBL_USER (ID) ON DELETE CASCADE
) COMMENT '사용자별 푸시 기기 토큰';

-- 발송할 때마다 사용자로 전부 찾는다.
CREATE INDEX IDX_PUSH_DEVICE_USER ON TBL_PUSH_DEVICE (USER_ID);
