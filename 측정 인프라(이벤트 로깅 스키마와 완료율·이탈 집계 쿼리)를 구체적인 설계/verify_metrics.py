import duckdb

con = duckdb.connect(':memory:')

# ── 스키마 (PostgreSQL 마이그레이션의 핵심 테이블을 DuckDB 방언으로) ──
con.execute("""
CREATE TABLE response_session (
    id BIGINT PRIMARY KEY,
    form_id VARCHAR,
    anon_session_id VARCHAR,
    experiment_arm VARCHAR,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    end_state VARCHAR,
    quality_flag VARCHAR,
    last_question_order INT,
    duration_sec INT
);
""")

# ── 샘플 데이터: 한 설문에 10세션 시작 ──
#   6 제출(그중 5 pass, 1 reject), 2 명시적 이탈, 2 진행중(타임아웃 대상)
rows = [
    # id, form, sess, arm, start, end, state, flag, lastq, dur
    (1,'F1','s1','C','2026-06-01 09:00:00','2026-06-01 09:03:00','submitted','pass',8,180),
    (2,'F1','s2','C','2026-06-01 09:10:00','2026-06-01 09:14:00','submitted','pass',8,240),
    (3,'F1','s3','C','2026-06-01 09:20:00','2026-06-01 09:22:00','submitted','pass',8,120),
    (4,'F1','s4','C','2026-06-01 10:00:00','2026-06-01 10:05:00','submitted','pass',8,300),
    (5,'F1','s5','C','2026-06-01 10:30:00','2026-06-01 10:33:00','submitted','pass',8,180),
    (6,'F1','s6','C','2026-06-01 11:00:00','2026-06-01 11:00:08','submitted','reject',8,8),   # 8초 = 불성실
    (7,'F1','s7','C','2026-06-01 11:10:00','2026-06-01 11:11:00','abandoned',None,3,None),    # 3번 문항서 이탈
    (8,'F1','s8','C','2026-06-01 11:20:00','2026-06-01 11:20:30','abandoned',None,1,None),    # 1번서 이탈
    (9,'F1','s9','C','2026-06-01 11:30:00',None,None,None,2,None),                            # 진행중(타임아웃 대상)
    (10,'F1','s10','C','2026-06-01 11:40:00',None,None,None,5,None),                          # 진행중
]
con.executemany("INSERT INTO response_session VALUES (?,?,?,?,?,?,?,?,?,?)", rows)

print("=== 1) 완료율 (제출 pass / 시작) ===")
print(con.execute("""
SELECT
    count(*)                                   AS started,
    count(*) FILTER (WHERE end_state='submitted')         AS submitted,
    count(*) FILTER (WHERE quality_flag='pass')           AS passed,
    round(100.0 * count(*) FILTER (WHERE quality_flag='pass') / count(*), 1) AS pass_rate_pct,
    round(100.0 * count(*) FILTER (WHERE end_state='submitted') / count(*), 1) AS submit_rate_pct
FROM response_session WHERE form_id='F1'
""").fetchdf().to_string(index=False))

print("\n=== 2) 질문별 이탈 분포 (이탈 세션의 마지막 응답 문항) ===")
print(con.execute("""
SELECT last_question_order AS dropped_at_q, count(*) AS sessions
FROM response_session
WHERE form_id='F1' AND end_state='abandoned'
GROUP BY last_question_order ORDER BY last_question_order
""").fetchdf().to_string(index=False))

print("\n=== 3) 불성실 응답률 (reject+hold / 제출) ===")
print(con.execute("""
SELECT
    count(*) FILTER (WHERE end_state='submitted') AS submitted,
    count(*) FILTER (WHERE quality_flag IN ('reject','hold')) AS bad,
    round(100.0 * count(*) FILTER (WHERE quality_flag IN ('reject','hold'))
          / nullif(count(*) FILTER (WHERE end_state='submitted'),0), 1) AS bad_rate_pct
FROM response_session WHERE form_id='F1'
""").fetchdf().to_string(index=False))

print("\n=== 4) 소요시간 중앙값 (제출 pass 건만) ===")
print(con.execute("""
SELECT
    median(duration_sec) AS median_sec,
    round(avg(duration_sec),0) AS avg_sec,
    min(duration_sec) AS min_sec, max(duration_sec) AS max_sec
FROM response_session
WHERE form_id='F1' AND quality_flag='pass'
""").fetchdf().to_string(index=False))

print("\n=== 5) 타임아웃 이탈 감지 (진행중이며 30분 무활동) ===")
# 기준 시각을 12:30로 가정 → s9(11:30 시작), s10(11:40 시작) 모두 30분+ 경과
print(con.execute("""
SELECT id, anon_session_id, started_at,
       end_state IS NULL AS still_open
FROM response_session
WHERE form_id='F1' AND ended_at IS NULL
  AND started_at < TIMESTAMP '2026-06-01 12:30:00' - INTERVAL 30 MINUTE
""").fetchdf().to_string(index=False))

# ── 검증 단언 ──
r = con.execute("""
SELECT count(*) started,
       count(*) FILTER (WHERE quality_flag='pass') passed,
       count(*) FILTER (WHERE end_state='abandoned') abandoned
FROM response_session WHERE form_id='F1'
""").fetchone()
assert r[0]==10 and r[1]==5 and r[2]==2, f"집계 불일치: {r}"
print("\n[검증] 시작10/통과5/이탈2 → 완료율(pass) 50% — 단언 통과 ✓")
