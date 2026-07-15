-- 크레딧 트랜잭션 스키마 (화폐 무결성)
-- 설계: 크레딧_동시성_상세설계.md
--   - credit_balance = 빠른 조회용 캐시 (available/escrow/version)
--   - credit_ledger  = append-only 원장(진실). balance = SUM(ledger.delta)
--   - 멱등: (reason, ref_id) UNIQUE 로 이중 적립 차단

CREATE TABLE credit_balance (
    user_id   VARCHAR(64) PRIMARY KEY,
    available BIGINT NOT NULL DEFAULT 0 CHECK (available >= 0),  -- 가용 잔액
    escrow    BIGINT NOT NULL DEFAULT 0 CHECK (escrow >= 0),     -- 설문 예치(잠금) 잔액
    version   BIGINT NOT NULL DEFAULT 0                          -- 낙관적 락용
);

CREATE TABLE credit_ledger (
    id            BIGSERIAL   PRIMARY KEY,
    user_id       VARCHAR(64) NOT NULL,
    delta         BIGINT      NOT NULL,        -- 잔액 변동(+적립 / -차감)
    reason        VARCHAR(40) NOT NULL,        -- SIGNUP_BONUS / ESCROW_DEPOSIT / SPEND_ESCROW / EARN_RESPONSE / BURN / REFUND
    ref_id        VARCHAR(64) NOT NULL,        -- 멱등 키(formId 또는 responseId)
    coeff_version INT         NOT NULL DEFAULT 1,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_ledger_reason_ref UNIQUE (reason, ref_id)
);
CREATE INDEX idx_ledger_user ON credit_ledger(user_id);

-- 스켈레톤 seed: 인증 모듈 미구현 → X-User-Id 헤더로 식별되는 유저 초기 잔액
INSERT INTO credit_balance(user_id, available, escrow) VALUES
    ('u-owner', 1000, 0),   -- 설문 제작자
    ('u-alice',   50, 0),   -- 응답자 A
    ('u-bob',     50, 0),   -- 응답자 B
    ('SYSTEM',     0, 0);   -- 소각분이 모이는 시스템 계정
