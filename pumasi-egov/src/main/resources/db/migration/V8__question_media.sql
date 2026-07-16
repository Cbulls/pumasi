-- 설명/이미지 블록용 필드 (응답 불필요 문항)
ALTER TABLE form_question ADD COLUMN body_html TEXT;
ALTER TABLE form_question ADD COLUMN image_url VARCHAR(1000);
