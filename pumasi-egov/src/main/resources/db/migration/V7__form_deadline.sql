-- 폼 마감일시: 네이버폼식 기간 제한. NULL이면 기한 없음(정원·수동 마감만 적용).
ALTER TABLE form ADD COLUMN closes_at TIMESTAMPTZ NULL;
