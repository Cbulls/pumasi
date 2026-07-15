-- 응답 수집 스키마
-- 설계: 마스터 설계문서 §3.4 (유효성/어뷰징 분리, 1인 1회)
--   - quality_flag: pass / hold / reject (reject도 데이터는 저장, 크레딧만 미지급)
--   - (form_id, respondent_id) UNIQUE 로 1인 1회 강제

CREATE TABLE survey_response (
    id              VARCHAR(36) PRIMARY KEY,
    form_id         VARCHAR(36) NOT NULL REFERENCES form(id),
    respondent_id   VARCHAR(64) NOT NULL,
    quality_flag    VARCHAR(10) NOT NULL,          -- pass / hold / reject
    elapsed_seconds INT         NOT NULL DEFAULT 0,
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_response_once UNIQUE (form_id, respondent_id)
);
CREATE INDEX idx_response_form ON survey_response(form_id);

CREATE TABLE survey_answer (
    id          BIGSERIAL   PRIMARY KEY,
    response_id VARCHAR(36) NOT NULL REFERENCES survey_response(id) ON DELETE CASCADE,
    question_id VARCHAR(36) NOT NULL,
    value       TEXT                                -- 다중선택(CHECKBOX)은 값마다 한 행
);
CREATE INDEX idx_answer_response ON survey_answer(response_id);
