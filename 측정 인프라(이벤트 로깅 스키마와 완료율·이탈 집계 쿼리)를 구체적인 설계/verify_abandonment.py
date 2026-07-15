import duckdb
con = duckdb.connect(':memory:')

# 이탈 감지 2-방식 조합 검증:
#  (A) 명시적: beforeunload/visibilitychange로 클라이언트가 abandoned 비콘 전송
#  (B) 추정: 비콘이 안 와도, 배치가 "마지막 이벤트 후 N분 무활동" 세션을 timeout 처리
con.execute("""
CREATE TABLE survey_event (
    session_id BIGINT, event_type VARCHAR, occurred_at TIMESTAMP
);
""")
# s20: 이벤트가 9:00,9:01에 있고 그 뒤 끊김 → 추정 이탈
# s21: 9:05에 시작만 하고 끊김 → 추정 이탈
# s22: 정상 제출
con.executemany("INSERT INTO survey_event VALUES (?,?,?)", [
    (20,'survey_started','2026-06-01 09:00:00'),
    (20,'question_answered','2026-06-01 09:01:00'),
    (21,'survey_started','2026-06-01 09:05:00'),
    (22,'survey_started','2026-06-01 09:10:00'),
    (22,'question_answered','2026-06-01 09:11:00'),
    (22,'survey_submitted','2026-06-01 09:13:00'),
])

# 배치 기준시각 09:40. 무활동 30분 임계.
print("=== 추정 이탈(timeout) 후보: 마지막 이벤트 후 30분+ & 미제출 ===")
print(con.execute("""
WITH last_ev AS (
    SELECT session_id,
           max(occurred_at) AS last_at,
           bool_or(event_type='survey_submitted') AS submitted
    FROM survey_event GROUP BY session_id
)
SELECT session_id, last_at, submitted
FROM last_ev
WHERE NOT submitted
  AND last_at < TIMESTAMP '2026-06-01 09:40:00' - INTERVAL 30 MINUTE
ORDER BY session_id
""").fetchdf().to_string(index=False))

r = con.execute("""
WITH last_ev AS (
    SELECT session_id, max(occurred_at) last_at,
           bool_or(event_type='survey_submitted') submitted
    FROM survey_event GROUP BY session_id)
SELECT count(*) FROM last_ev
WHERE NOT submitted AND last_at < TIMESTAMP '2026-06-01 09:40:00' - INTERVAL 30 MINUTE
""").fetchone()
assert r[0]==2, f"이탈 후보 수 불일치: {r}"
print("\n[검증] s20,s21 두 건이 추정 이탈로 감지 — 단언 통과 ✓")
print("(s22는 제출했으므로 제외됨)")
