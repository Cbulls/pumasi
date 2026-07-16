-- RADIO 조건부 분기 규칙 + 공개 공유 토큰
ALTER TABLE form_question ADD COLUMN branch_rules JSONB;
ALTER TABLE form ADD COLUMN share_token VARCHAR(64) UNIQUE;

CREATE INDEX idx_form_share_token ON form(share_token) WHERE share_token IS NOT NULL;
