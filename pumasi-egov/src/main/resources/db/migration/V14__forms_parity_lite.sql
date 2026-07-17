-- Forms 패리티(라이트): 제출 완료 메시지 · 기타 보기 · 선택지 섞기

ALTER TABLE form
    ADD COLUMN IF NOT EXISTS confirmation_message VARCHAR(500);

ALTER TABLE form_question
    ADD COLUMN IF NOT EXISTS allow_other BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE form_question
    ADD COLUMN IF NOT EXISTS shuffle_options BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN form.confirmation_message IS '응답 제출 후 표시할 커스텀 안내(NULL이면 기본 문구)';
COMMENT ON COLUMN form_question.allow_other IS 'RADIO/CHECKBOX: 기타 직접입력 허용';
COMMENT ON COLUMN form_question.shuffle_options IS '응답 화면에서만 보기 순서 셔플';
