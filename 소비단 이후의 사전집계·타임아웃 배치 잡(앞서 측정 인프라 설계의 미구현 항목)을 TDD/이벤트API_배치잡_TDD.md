# 품앗이폼 — 사전집계·타임아웃 배치 잡 (TDD) v1.0

측정 인프라 소비단 이후의 두 배치 잡. 측정인프라 설계의 미구현 항목을 TDD로 완성.

---

## 0. 두 잡과 각각의 함정

| 잡 | 책임 | 핵심 함정 |
|---|---|---|
| 사전집계 | response_session → form_metrics_daily(완료율·이탈·소요시간) | 재실행 멱등성(두 번 돌려도 두 배 안 됨) |
| 타임아웃 | sendBeacon 유실 대비, 무활동 세션을 timeout 마감 | 경계 조건 + 경합(마감 직전 제출) |

---

## 1. 사전집계 잡

### 검증 규칙 (AggregationJobTest)
| ID | 규칙 |
|---|---|
| A1 | started/submitted/pass/abandoned 카운트 정확 |
| A2 | 완료율은 pass 기준(불성실 제외), submitted와 구분 저장 |
| A3 | 소요시간 중앙값은 pass 건만 |
| A4 | ★재실행 멱등★ 같은 날 두 번 집계해도 두 배 안 됨 |
| A5 | arm(실험군)별 분리 집계 |
| A6 | 진행중 세션은 started에 포함, 종료 카운트엔 미포함 |

### 멱등성의 원리
증분 누적이 아니라 **해당 날짜를 전체 재계산 → upsert(ON CONFLICT DO UPDATE)**.
몇 번 돌려도 같은 값. 크론 재시도·수동 백필 안전.

### Spring
- `MetricsAggregationJob`: @Scheduled(cron 매일 02:10) + aggregate(date) 수동 호출용.
- `MetricsAggregationRepository.upsertDailyMetrics`: 네이티브 upsert. FILTER로 조건부 카운트,
  percentile_cont로 중앙값, ON CONFLICT로 멱등.

---

## 2. 타임아웃 잡

### 검증 규칙 (TimeoutJobTest)
| ID | 규칙 |
|---|---|
| T1 | 마지막 이벤트 후 임계 초과 + 미종료 → timeout 마감 |
| T2 | ★경계★ 임계 미만은 마감 안 함 |
| T3 | 이미 종료(submitted/abandoned)는 안 건드림 |
| T4 | ended_at = 마지막 이벤트 시각(실제 이탈 시점 근사) |
| T5 | ★멱등★ 두 번 돌려도 이미 timeout된 건 재처리 안 함 |
| T6 | ★경합★ 마감 직전 제출되면 timeout 적용 안 함 |

### 경합 방어(T6)의 원리
별도 SELECT 후 개별 UPDATE 하지 않는다. **조건부 UPDATE 한 방**으로:
```sql
UPDATE response_session SET end_state='timeout', ended_at=last_event_at
WHERE end_state IS NULL AND last_event_at < :cutoff
```
`WHERE end_state IS NULL`이 마감 직전 제출을 막는다. 그 사이 제출되면 이 조건에 안 걸려
자동 스킵. 크레딧·세션 경합과 같은 원칙(check-then-act → 원자적 조건부 연산).

### Spring
- `SessionTimeoutJob`: @Scheduled(5분마다) + 임계 설정값(기본 30분).
- `ResponseSessionTimeoutRepository.closeTimedOut`: 조건부 UPDATE 한 방.
- V3 마이그레이션: `last_event_at` 컬럼 + 미종료 세션 부분 인덱스 추가.

---

## 3. TDD 사이클 기록 (실제 실행)

| 잡 | RED | GREEN | 비고 |
|---|---|---|---|
| 사전집계 | 컴파일 실패 확인 | 9 pass | upsert로 멱등 |
| 타임아웃 | 컴파일 실패 확인 | 9 pass | — |

> 디버깅 사례: 타임아웃 T1/T2가 처음 실패. 원인은 구현 버그가 아니라 **테스트의 모델 오류** —
> 진행중 세션을 end_state="started"로 표현했으나 실제론 null이어야 함(started는 별도 boolean).
> 후보 조회가 빈 것을 디버깅으로 확인하고 테스트를 바로잡았다. 실패를 맹목 통과시키지 않고
> 원인을 추적한 결과.

전체 회귀: 핵심13 + 운영11 + 큐12 + 배치18 = **54 테스트 통과**.

---

## 4. 검증 상태 (정직한 한계)
- 집계 정확성·멱등(A1~A6), 타임아웃 경계·경합·멱등(T1~T6): 순수 Java로 실제 RED→GREEN 검증 ✓
- 전체 회귀 54 테스트 통과 ✓
- Spring 잡/네이티브 쿼리/V3 마이그레이션: 코드 작성·로직 대조 ✓, 빌드 미실행 ⚠️
- 네이티브 SQL(percentile_cont, ON CONFLICT, 조건부 UPDATE)·@Scheduled·BatchJobsIT:
  실 PostgreSQL에서 검증 필요 ⚠️ (테스트 코드는 작성됨)
- last_event_at 갱신: EventIngestService에서 이벤트 적재 시 now()로 갱신하는 로직 추가 필요(TODO)

---

## 5. 산출물
| 파일 | 내용 |
|---|---|
| `tdd4/AggregationJobTest.java` | 집계 정확성·멱등 테스트 |
| `tdd4/AggregationJob.java` | 집계 로직(전체 재계산 + upsert) |
| `tdd4/TimeoutJobTest.java` | 경계·경합·멱등 테스트 |
| `tdd4/TimeoutJob.java` | 타임아웃 로직(조건부 마감) |
| `spring/.../batch/MetricsAggregationJob.java` | @Scheduled 집계 잡 |
| `spring/.../batch/MetricsAggregationRepository.java` | 네이티브 upsert |
| `spring/.../batch/SessionTimeoutJob.java` | @Scheduled 타임아웃 잡 |
| `spring/.../batch/ResponseSessionTimeoutRepository.java` | 조건부 UPDATE |
| `spring/.../db/migration/V3__session_last_event.sql` | last_event_at 컬럼 |
| `spring/.../batch/BatchJobsIT.java` | 멱등·경합 통합 테스트 |
