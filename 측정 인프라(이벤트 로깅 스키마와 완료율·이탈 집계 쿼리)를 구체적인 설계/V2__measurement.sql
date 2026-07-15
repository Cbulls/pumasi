-- 품앗이폼 측정 인프라 — PostgreSQL 마이그레이션 (Flyway: V2__measurement.sql)
-- 목적: baseline 산출(완료율/이탈률/소요시간/불성실률)에 필요한 응답자 여정 이벤트 로깅
--
-- 설계 원칙:
--  1) 완료율의 분모는 "응답 시작" 수다. 시작이 기록되지 않으면 완료율을 못 잰다.
--  2) 원본 이벤트는 append-only. 집계는 별도 배치가 사전집계 테이블로 만든다(대시보드 부하 분리).
--  3) 비로그인 응답자도 추적해야 하므로 익명 세션 키(anon_session_id)를 함께 둔다.

-- ── 응답 세션: 한 응답자가 한 설문을 "시작"한 단위 (완료율의 분모) ──
CREATE TABLE response_session (
    id              BIGSERIAL PRIMARY KEY,
    form_id         UUID        NOT NULL,
    -- 로그인 사용자면 user_id, 아니면 익명 세션 키로 식별
    respondent_id   UUID        NULL,
    anon_session_id VARCHAR(64) NOT NULL,
    experiment_arm  VARCHAR(32) NULL,          -- A/B 군 (실험 시 채움)
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 종료 상태: NULL=진행중, 'submitted'/'abandoned'/'timeout'
    ended_at        TIMESTAMPTZ NULL,
    end_state       VARCHAR(16) NULL,
    -- 제출까지 도달한 경우 RESPONSE 와 연결
    response_id     UUID        NULL,
    quality_flag    VARCHAR(16) NULL,          -- pass/hold/reject (제출 시 판정)
    last_question_order INT     NULL,          -- 마지막으로 응답한 문항 순서(이탈 지점 분석)
    duration_sec    INT         NULL,          -- 제출 시 = ended_at - started_at
    device_type     VARCHAR(16) NULL,          -- mobile/desktop/tablet
    UNIQUE (form_id, anon_session_id)          -- 한 세션이 같은 폼을 중복 시작하지 않게
);
CREATE INDEX idx_session_form     ON response_session (form_id);
CREATE INDEX idx_session_started  ON response_session (started_at);
CREATE INDEX idx_session_state    ON response_session (end_state);

-- ── 이벤트 로그: 여정의 모든 지점 (append-only) ──
CREATE TABLE survey_event (
    id              BIGSERIAL PRIMARY KEY,
    session_id      BIGINT      NOT NULL REFERENCES response_session(id),
    form_id         UUID        NOT NULL,
    event_type      VARCHAR(32) NOT NULL,      -- 아래 CHECK 참조
    question_id     UUID        NULL,          -- question_answered 일 때
    question_order  INT         NULL,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    meta            JSONB       NULL,          -- 추가 맥락(클라이언트 타임스탬프 등)
    CONSTRAINT chk_event_type CHECK (event_type IN (
        'survey_viewed',      -- 설문 페이지 노출(시작 전)
        'survey_started',     -- 첫 상호작용 = 완료율 분모
        'question_answered',  -- 문항 응답 1건
        'survey_submitted',   -- 제출
        'survey_abandoned'    -- 명시적/추정 이탈
    ))
);
CREATE INDEX idx_event_session ON survey_event (session_id);
CREATE INDEX idx_event_type    ON survey_event (event_type, occurred_at);

-- ── 일별 사전집계 (배치가 채움; 대시보드/실험은 이걸 읽음) ──
CREATE TABLE form_metrics_daily (
    metric_date     DATE        NOT NULL,
    form_id         UUID        NOT NULL,
    experiment_arm  VARCHAR(32) NULL,
    started_cnt     INT         NOT NULL DEFAULT 0,
    submitted_cnt   INT         NOT NULL DEFAULT 0,   -- 전체 제출
    pass_cnt        INT         NOT NULL DEFAULT 0,   -- quality_flag=pass
    abandoned_cnt   INT         NOT NULL DEFAULT 0,
    median_duration_sec INT     NULL,
    PRIMARY KEY (metric_date, form_id, experiment_arm)
);
