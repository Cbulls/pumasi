-- 폼 빌더 스키마 (form / section / question / option)
-- 설계: 품앗이폼_마스터_설계문서 V1 (폼 애그리거트)

CREATE TABLE form (
    id            VARCHAR(36)  PRIMARY KEY,
    owner_id      VARCHAR(64)  NOT NULL,
    title         VARCHAR(300) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',   -- DRAFT / ACTIVE / CLOSED
    cost_credits  INT          NOT NULL DEFAULT 0,          -- 응답 1건당 비용(publish 시 자동 산출)
    max_responses INT          NOT NULL DEFAULT 100,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_form_owner ON form(owner_id);

CREATE TABLE form_section (
    id          VARCHAR(36)  PRIMARY KEY,
    form_id     VARCHAR(36)  NOT NULL REFERENCES form(id) ON DELETE CASCADE,
    order_index INT          NOT NULL,
    title       VARCHAR(300)
);
CREATE INDEX idx_section_form ON form_section(form_id);

CREATE TABLE form_question (
    id          VARCHAR(36)  PRIMARY KEY,
    section_id  VARCHAR(36)  NOT NULL REFERENCES form_section(id) ON DELETE CASCADE,
    form_id     VARCHAR(36)  NOT NULL REFERENCES form(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,                      -- SHORT_TEXT / LONG_TEXT / RADIO / CHECKBOX / LINEAR_SCALE
    title       VARCHAR(500) NOT NULL,
    required    BOOLEAN      NOT NULL DEFAULT false,
    order_index INT          NOT NULL,
    min_select  INT,
    max_select  INT,
    min_length  INT,
    max_length  INT,
    regex       VARCHAR(300),
    scale_min   INT,
    scale_max   INT
);
CREATE INDEX idx_question_form ON form_question(form_id);

CREATE TABLE form_question_option (
    id          VARCHAR(36)  PRIMARY KEY,
    question_id VARCHAR(36)  NOT NULL REFERENCES form_question(id) ON DELETE CASCADE,
    label       VARCHAR(300) NOT NULL,
    order_index INT          NOT NULL
);
CREATE INDEX idx_option_question ON form_question_option(question_id);
