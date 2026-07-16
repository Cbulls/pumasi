-- 응답 세션: 응답 시작 시각을 서버가 기록해 소요시간을 서버에서 계산한다.
--   기존에는 클라이언트가 보낸 elapsedSeconds를 신뢰 → 고속제출 판정 우회 가능했다.
--   (form_id, respondent_id) 1행: 재시작하면 started_at 갱신.

CREATE TABLE response_session (
    form_id       VARCHAR(36) NOT NULL REFERENCES form(id) ON DELETE CASCADE,
    respondent_id VARCHAR(64) NOT NULL,
    started_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (form_id, respondent_id)
);
