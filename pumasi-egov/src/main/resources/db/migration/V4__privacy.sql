-- 개인정보보호 보강
--  1) 응답 익명화: 외부 노출용 익명 라벨(실제 respondent_id는 정산·1인1회 내부용으로만)
--  2) 동의 수집: 개인정보 수집·이용 동의 시각

ALTER TABLE survey_response ADD COLUMN anon_label VARCHAR(20);
ALTER TABLE survey_response ADD COLUMN consent_at TIMESTAMPTZ;

-- 기존 행(있다면) 익명 라벨 백필
UPDATE survey_response
   SET anon_label = '익명-' || substr(id, 1, 6)
 WHERE anon_label IS NULL;
