-- V11: 문항 이미지 에셋 규약
-- image_url 에는 기준 경로만 저장: /pmsi/form/{formId}/media/{assetId}
-- 변형: ?v=thumb|display|orig  (thumb/display/orig 는 WebP 파생본)

COMMENT ON COLUMN form_question.image_url IS
    '문항 이미지 에셋 기준 URL. /pmsi/form/{formId}/media/{assetId} — ?v=thumb|display|orig';
