# 품앗이폼 — 가드레일 대시보드 백엔드 (TDD) v1.0

가드레일 기획을 실제 백엔드로 구체화. "예쁜 지표"가 아니라 "망가지기 전에 멈추는" 시스템.

---

## 0. 핵심 사상 (기획에서)

- 가드레일은 **작동한다, 보기만 하지 않는다.** 임계 초과 시 자동 대응(중단/알림).
- **모든 지표는 통제군 대비 상대값.** 절대값은 외부 요인으로 출렁이므로.
- **자동 vs 수동 구분.** 명확한 위반은 자동 차단, 오탐 위험 큰 것(자전거래)은 검토 큐.
- **중단은 자동, 재개는 수동.** 오탐으로 멈춘 실험이 자동 재개되며 반복되지 않게.

---

## 1. 임계 판정 (GuardrailEvaluationTest, 8 테스트)

지표 스냅샷 → 자동 대응 판정.

| ID | 규칙 |
|---|---|
| G1 | ★상대값★ 실험군 지표를 통제군과 비교(차이가 임계 초과인지) |
| G2 | 불성실률 통제군 대비 +5%p 초과 → 자동 중단 |
| G3 | 7일 재방문율 통제군 대비 -3%p 미만 → 자동 중단 |
| G4 | ★유의성 게이트★ 표본 부족 시 임계 초과여도 보류(우연 변동 배제) |
| G5 | ★자동 vs 수동★ 자전거래 의심 → 검토 큐(자동 중단 안 함) |
| G6 | ★쿨다운★ 최근 중단된 군은 쿨다운 동안 재판정 안 함 |
| G7 | 정상 범위 → NONE |

> G4가 핵심 보호장치: 불성실률이 +6%p로 임계를 넘어도 표본이 30이면 우연일 수 있다.
> 두 비율 z-검정으로 유의성을 확인해, 표본 부족 시 멀쩡한 실험을 오탐 중단으로부터 보호.
> (표본 크기 계산기와 같은 통계 기반)

판정 우선순위: 쿨다운 → 자전거래(검토 큐) → 임계+유의성(자동 중단) → NONE.

## 2. 자동 대응 오케스트레이션 (GuardrailOrchestrationTest, 12 테스트)

판정을 실제로 집행.

| ID | 규칙 |
|---|---|
| O1 | AUTO_SUSPEND → arm 트래픽 0 + 기본 계수 복귀(feature flag) + 알림 + 중단시각 기록 |
| O2 | ★중단 자동, 재개 수동★ resume()은 승인자(approverId) 필수. 자동 재개 없음 |
| O3 | MANUAL_REVIEW → 검토 큐 적재 + 알림(중단 안 함) |
| O4 | ★알림 멱등★ 같은 arm 같은 사유는 한 번만 알림(알림 폭풍 방지) |
| O5 | NONE/COOLDOWN → 무동작 |
| O6 | 중단 집행 시 중단 시각 기록(쿨다운 근거) |

> O2의 비대칭이 핵심: 위험 감지는 즉시(자동), 정상 복귀는 신중히(수동 승인 + 쿨다운).
> 오탐으로 멈췄다 자동 재개됐다 반복하면 실험 데이터가 오염된다.

---

## 3. Spring 구현

### 흐름
```
GuardrailMonitorJob (@Scheduled, 체크포인트)
 → metricRepo.loadActiveArmMetrics()   측정 인프라+크레딧에서 실험군 지표 집계
 → evaluator.evaluate()                임계+유의성 판정
 → orchestrator.apply()                자동 대응 집행
```

### API (관리자 전용)
```
GET  /api/admin/guardrail/metrics           실험군 지표 + 현재 판정
GET  /api/admin/guardrail/review-queue       검토 큐(자전거래 등)
POST /api/admin/guardrail/arms/{arm}/resume  수동 재개(승인자 필요, 없으면 400)
```

### 컴포넌트
- `GuardrailEvaluator`: 임계+유의성 판정(순수 로직, 검증됨)
- `GuardrailOrchestrator`: 자동 대응 집행(순수 로직, 검증됨)
- `GuardrailMonitorJob`: @Scheduled 주기 평가
- `GuardrailDashboardController`: 조회 + 수동 재개
- 포트: `FeatureFlagPort`(arm 트래픽), `AlertPort`(메시지 큐 알림),
  `ReviewQueuePort`(검토 큐), `GuardrailMetricRepository`(지표 집계)

### 지표 집계
별도 테이블 없이 기존 측정 인프라에서 조회:
- 불성실률: response_session.quality_flag IN (HOLD,REJECT) 비율
- 재방문율: 가입/활성 후 7일 내 재방문(이벤트 기반)
- 모두 experiment_arm으로 그룹핑 → 통제군과 차이 계산

### 알림
임계 초과 → AlertPort → 메시지 큐 발행 → Slack/이메일/PagerDuty(기획).
멱등(O4)으로 같은 위반 중복 알림 차단.

---

## 4. 검증 상태 (정직한 한계)
- 임계 판정·오케스트레이션: 순수 Java로 실제 RED→GREEN, 20 테스트 통과 ✅
- Spring 잡/컨트롤러/포트: 코드 작성·로직 대조 ✅, 빌드 미실행 ⚠️
- 미세 보강 필요:
  - GuardrailEvaluator/Orchestrator에 @Component/@Service + 포트 빈 등록 ⚠️
  - 포트 구현체(FeatureFlag=실제 flag 시스템, Alert=메시지 큐, MetricRepo=집계 SQL) ⬜
  - 알림 멱등을 인메모리 Set → DB(guardrail_alert_log)로 영속화(다중 인스턴스) ⬜
  - 중단 상태를 인메모리 → DB(guardrail_arm_state)로(재시작 후에도 유지) ⬜
  - 체크포인트 기반 평가(기획: peeking 방지 — 아무 때나 평가하면 다중비교로 거짓양성↑)
  - 통계적 유의성을 더 정교하게(현재 단순 z-검정 — 순차검정/베이지안 고려)

---

## 5. 산출물
| 파일 | 내용 |
|---|---|
| `tdd/GuardrailEvaluationTest.java` | 임계 판정 8 테스트 |
| `tdd/GuardrailEvaluator.java` | 판정 로직(상대값·유의성·쿨다운) |
| `tdd/GuardrailOrchestrationTest.java` | 오케스트레이션 12 테스트 |
| `tdd/GuardrailOrchestrator.java` | 집행 로직(중단 자동/재개 수동/알림 멱등) |
| `spring/.../GuardrailMonitorJob.java` | @Scheduled 주기 평가 |
| `spring/.../GuardrailDashboardController.java` | 대시보드 API + 지표 포트 |
| `spring/.../db/migration/V7__guardrail.sql` | 중단 상태·검토 큐·알림 멱등 |
