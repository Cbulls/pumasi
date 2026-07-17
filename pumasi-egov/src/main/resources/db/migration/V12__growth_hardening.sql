-- V12: 시스템 고도화 Phase 1/2 기반
--   - 주의 문항(attention_answer): RADIO 전용 기대 정답. 불일치 시 품질 reject
--   - form.status에 PAUSED(가드레일 자동 일시정지) 허용
--   - 크레딧 PURCHASE(충전) 사유를 available 불변식에 추가
--   - 응답 퍼널 이벤트(survey_event): view/start/submit 측정
--   - 비동기 내보내기 잡(export_job)
--   - 언락 조인 성능 인덱스

-- 1) 주의 문항
ALTER TABLE form_question ADD COLUMN attention_answer VARCHAR(500);
COMMENT ON COLUMN form_question.attention_answer IS
    'RADIO 전용 주의 검증 정답. 설정 시 응답이 이 값과 다르면 품질 reject.';

-- 2) 가드레일 일시정지 상태
ALTER TABLE form DROP CONSTRAINT chk_form_status;
ALTER TABLE form
    ADD CONSTRAINT chk_form_status
    CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'CLOSED'));

-- 3) available 불변식에 PURCHASE 추가
COMMENT ON TABLE credit_ledger IS
    'append-only 원장. available = SUM(delta) WHERE reason IN (GENESIS,ESCROW_DEPOSIT,ESCROW_REFUND,EARN_RESPONSE,SIGNUP_BONUS,BURN,PURCHASE). SPEND_ESCROW는 legacy(escrow 전용, available 합산 제외).';

-- 4) 응답 퍼널 이벤트
CREATE TABLE survey_event (
    id         BIGSERIAL    PRIMARY KEY,
    form_id    VARCHAR(36)  NOT NULL REFERENCES form(id) ON DELETE CASCADE,
    user_id    VARCHAR(64),
    event_type VARCHAR(20)  NOT NULL CHECK (event_type IN ('view', 'start', 'submit')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_event_form_type ON survey_event(form_id, event_type);

-- 5) 비동기 내보내기 잡
CREATE TABLE export_job (
    id          VARCHAR(36)  PRIMARY KEY,
    form_id     VARCHAR(36)  NOT NULL REFERENCES form(id) ON DELETE CASCADE,
    owner_id    VARCHAR(64)  NOT NULL REFERENCES user_account(user_id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'QUEUED'
                CHECK (status IN ('QUEUED', 'RUNNING', 'DONE', 'FAILED')),
    storage_key VARCHAR(300),
    file_name   VARCHAR(300),
    error       VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at TIMESTAMPTZ
);
CREATE INDEX idx_export_job_form ON export_job(form_id, created_at DESC);

-- 6) 언락(상호 응답) 조인 성능
CREATE INDEX idx_response_respondent ON survey_response(respondent_id);
