-- 사용자 관점 UX: 매직링크 인증 · 알림 · 활동 이력 지원 인덱스

-- 이메일 기반 실계정 (시드 데모 계정은 email NULL 유지)
ALTER TABLE user_account
    ADD COLUMN IF NOT EXISTS email VARCHAR(320);

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_account_email
    ON user_account (lower(email))
    WHERE email IS NOT NULL;

-- 매직링크 일회용 토큰 (TTL 15분)
CREATE TABLE IF NOT EXISTS magic_link_token (
    token      VARCHAR(64)  PRIMARY KEY,
    email      VARCHAR(320) NOT NULL,
    user_id    VARCHAR(64)  REFERENCES user_account(user_id),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_magic_link_email ON magic_link_token(lower(email));

-- 인앱 알림
CREATE TABLE IF NOT EXISTS user_notification (
    id         VARCHAR(64)  PRIMARY KEY,
    user_id    VARCHAR(64)  NOT NULL REFERENCES user_account(user_id),
    type       VARCHAR(40)  NOT NULL,
    title      VARCHAR(200) NOT NULL,
    body       VARCHAR(500),
    link_url   VARCHAR(500),
    ref_id     VARCHAR(64),
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON user_notification(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_user_unread
    ON user_notification(user_id)
    WHERE read_at IS NULL;

COMMENT ON COLUMN user_notification.type IS
    'NEW_RESPONSE | UNLOCK_AVAILABLE | FORM_PAUSED | HOLD_REVIEW';
