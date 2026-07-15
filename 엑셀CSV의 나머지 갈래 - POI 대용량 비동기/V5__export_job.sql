-- 품앗이폼 비동기 내보내기 작업 (Flyway: V5__export_job.sql)
-- ExportJobManager의 상태를 여러 인스턴스가 공유하도록 DB에 저장.

CREATE TABLE export_job (
    id            VARCHAR(40) PRIMARY KEY,
    form_id       VARCHAR(36) NOT NULL,
    user_id       VARCHAR(36) NOT NULL,
    format        VARCHAR(8)  NOT NULL,        -- xlsx / csv
    expand        BOOLEAN     NOT NULL DEFAULT false,
    status        VARCHAR(12) NOT NULL,        -- PENDING/RUNNING/COMPLETED/FAILED
    download_url  TEXT,
    fail_reason   TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ,
    CONSTRAINT chk_status CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED'))
);

-- 중복 합치기: 같은 (form,user,format,expand)로 진행 중(PENDING/RUNNING) 작업은 하나만.
-- 부분 유니크 인덱스로 진행 중 작업만 유일성 강제(종료된 작업은 여러 개 허용).
CREATE UNIQUE INDEX uq_export_job_inflight
    ON export_job (form_id, user_id, format, expand)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE INDEX idx_export_job_user ON export_job (user_id);

-- 만료 정리: completed_at + TTL 지난 COMPLETED 작업과 저장소 파일은 배치로 삭제.
-- (별도 정리 잡 — SessionTimeoutJob과 유사한 @Scheduled)
