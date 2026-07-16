-- V10: DB 무결성 보강
--   - SYSTEM 계정 + FK (owner/respondent/credit/answer.question)
--   - status/quality/max_responses CHECK
--   - anon_label/consent_at 마무리
--   - 핫 인덱스, 중복 share_token 인덱스 제거
--   - GENESIS 원장 백필: available = SUM(available-affecting ledger)
--     available-affecting: GENESIS, ESCROW_DEPOSIT, ESCROW_REFUND, EARN_RESPONSE, SIGNUP_BONUS, BURN
--     SPEND_ESCROW 는 legacy(escrow 차감 전용)로 available 합산에서 제외

-- 1) 시스템 계정
INSERT INTO user_account(user_id, display_name)
VALUES ('SYSTEM', '시스템')
ON CONFLICT (user_id) DO NOTHING;

-- 2) 고아 credit_balance / 응답 방지용: 잔액만 있고 계정 없는 경우 계정 생성(데모 안전망)
INSERT INTO user_account(user_id, display_name)
SELECT b.user_id, b.user_id
  FROM credit_balance b
 WHERE NOT EXISTS (SELECT 1 FROM user_account u WHERE u.user_id = b.user_id)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_account(user_id, display_name)
SELECT DISTINCT r.respondent_id, r.respondent_id
  FROM survey_response r
 WHERE NOT EXISTS (SELECT 1 FROM user_account u WHERE u.user_id = r.respondent_id)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_account(user_id, display_name)
SELECT DISTINCT f.owner_id, f.owner_id
  FROM form f
 WHERE NOT EXISTS (SELECT 1 FROM user_account u WHERE u.user_id = f.owner_id)
ON CONFLICT (user_id) DO NOTHING;

-- 3) FK
ALTER TABLE form
    ADD CONSTRAINT fk_form_owner
    FOREIGN KEY (owner_id) REFERENCES user_account(user_id);

ALTER TABLE survey_response
    ADD CONSTRAINT fk_response_respondent
    FOREIGN KEY (respondent_id) REFERENCES user_account(user_id);

ALTER TABLE response_session
    ADD CONSTRAINT fk_session_respondent
    FOREIGN KEY (respondent_id) REFERENCES user_account(user_id);

ALTER TABLE credit_balance
    ADD CONSTRAINT fk_balance_user
    FOREIGN KEY (user_id) REFERENCES user_account(user_id);

ALTER TABLE credit_ledger
    ADD CONSTRAINT fk_ledger_user
    FOREIGN KEY (user_id) REFERENCES user_account(user_id);

-- 답변 → 문항 (삭제 RESTRICT: 응답 있는 문항 삭제 방지)
ALTER TABLE survey_answer
    ADD CONSTRAINT fk_answer_question
    FOREIGN KEY (question_id) REFERENCES form_question(id);

-- 4) CHECK
ALTER TABLE form
    ADD CONSTRAINT chk_form_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'CLOSED'));

ALTER TABLE form
    ADD CONSTRAINT chk_form_max_responses
    CHECK (max_responses >= 1);

ALTER TABLE form
    ADD CONSTRAINT chk_form_cost_credits
    CHECK (cost_credits >= 0);

ALTER TABLE survey_response
    ADD CONSTRAINT chk_response_quality
    CHECK (quality_flag IN ('pass', 'hold', 'reject'));

ALTER TABLE survey_response
    ADD CONSTRAINT chk_response_elapsed
    CHECK (elapsed_seconds >= 0);

-- version: 변경 카운터(비관적 락과 병행, CAS 미사용). CHECK만 보강.
ALTER TABLE credit_balance
    ADD CONSTRAINT chk_balance_version
    CHECK (version >= 0);

COMMENT ON COLUMN credit_balance.version IS
    '변경 카운터. 동시성은 SELECT FOR UPDATE(비관적 락). CAS(낙관적 락) 미사용.';

COMMENT ON TABLE credit_ledger IS
    'append-only 원장. available = SUM(delta) WHERE reason IN (GENESIS,ESCROW_DEPOSIT,ESCROW_REFUND,EARN_RESPONSE,SIGNUP_BONUS,BURN). SPEND_ESCROW는 legacy(escrow 전용, available 합산 제외).';

-- 5) privacy 마무리
UPDATE survey_response
   SET anon_label = '익명-' || substr(id, 1, 6)
 WHERE anon_label IS NULL OR anon_label = '';

UPDATE survey_response
   SET consent_at = COALESCE(consent_at, submitted_at, now())
 WHERE consent_at IS NULL;

ALTER TABLE survey_response
    ALTER COLUMN anon_label SET NOT NULL;

ALTER TABLE survey_response
    ALTER COLUMN consent_at SET NOT NULL;

ALTER TABLE survey_response
    ADD CONSTRAINT uq_response_anon_per_form UNIQUE (form_id, anon_label);

-- 6) 핫 인덱스
CREATE INDEX idx_response_form_pass
    ON survey_response(form_id)
    WHERE quality_flag = 'pass';

CREATE INDEX idx_question_section
    ON form_question(section_id);

CREATE INDEX idx_form_active_created
    ON form(created_at DESC)
    WHERE status = 'ACTIVE';

-- UNIQUE(share_token)와 중복인 partial index 제거
DROP INDEX IF EXISTS idx_form_share_token;

-- 7) GENESIS 원장 백필: available - (기존 available-affecting SUM)
INSERT INTO credit_ledger(user_id, delta, reason, ref_id, coeff_version)
SELECT b.user_id,
       b.available - COALESCE((
           SELECT SUM(l.delta)
             FROM credit_ledger l
            WHERE l.user_id = b.user_id
              AND l.reason IN (
                  'GENESIS', 'ESCROW_DEPOSIT', 'ESCROW_REFUND',
                  'EARN_RESPONSE', 'SIGNUP_BONUS', 'BURN'
              )
       ), 0),
       'GENESIS',
       'genesis-' || b.user_id,
       1
  FROM credit_balance b
 WHERE NOT EXISTS (
           SELECT 1 FROM credit_ledger x
            WHERE x.reason = 'GENESIS'
              AND x.ref_id = 'genesis-' || b.user_id
       )
   AND (
       b.available - COALESCE((
           SELECT SUM(l.delta)
             FROM credit_ledger l
            WHERE l.user_id = b.user_id
              AND l.reason IN (
                  'GENESIS', 'ESCROW_DEPOSIT', 'ESCROW_REFUND',
                  'EARN_RESPONSE', 'SIGNUP_BONUS', 'BURN'
              )
       ), 0)
   ) <> 0;
