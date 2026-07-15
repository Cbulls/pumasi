-- 인증/인가: 계정 + 세션 토큰
--   X-User-Id 헤더를 그대로 신뢰하던 방식을 제거하고, 로그인으로 발급한
--   토큰(auth_session)으로만 사용자를 식별한다.

CREATE TABLE user_account (
    user_id      VARCHAR(64)  PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 데모 계정(기존 credit_balance seed와 동일 식별자)
INSERT INTO user_account(user_id, display_name) VALUES
    ('u-owner', '제작자'),
    ('u-alice', '앨리스'),
    ('u-bob',   '밥');

CREATE TABLE auth_session (
    token      VARCHAR(64)  PRIMARY KEY,
    user_id    VARCHAR(64)  NOT NULL REFERENCES user_account(user_id),
    issued_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_session_user ON auth_session(user_id);
