-- 객관식/체크박스 그리드: 행 라벨(JSONB). 열은 form_question_option 재사용.

ALTER TABLE form_question
    ADD COLUMN IF NOT EXISTS row_labels JSONB;

COMMENT ON COLUMN form_question.row_labels IS
    'MULTIPLE_CHOICE_GRID / CHECKBOX_GRID 행 라벨 배열. 열은 form_question_option.';
