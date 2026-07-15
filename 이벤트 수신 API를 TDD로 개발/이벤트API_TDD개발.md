# 품앗이폼 — 이벤트 수신 API (TDD 개발) v1.0

`POST /api/events` 측정 인프라 이벤트 수신 엔드포인트. Red-Green-Refactor로 개발.

---

## 0. TDD 사이클 기록 (실제 실행)

이 환경은 Maven Central 차단으로 Spring을 직접 빌드할 수 없다. 그래서 핵심 비즈니스 로직
(검증·멱등·전이 규칙)을 프레임워크에서 분리해 순수 Java로 만들고, **실제로 RED→GREEN→REFACTOR
사이클을 돌려 검증**했다. Spring 코드는 이 검증된 로직을 감싸는 얇은 껍데기다.

| 사이클 | 단계 | 내용 | 결과 |
|---|---|---|---|
| 1 | RED | EventLogicTest 작성(구현 전) | 컴파일 실패 확인 ✓ |
| 1 | GREEN | EventValidator + EventService 최소 구현 | 12 pass / 0 fail ✓ |
| 2 | RED | R7(순서 역전) 경계 테스트 추가 | 1 fail 확인 ✓ |
| 2 | GREEN | last_question_order를 MAX 유지로 수정 | 1 pass, 기존 12 회귀 없음 ✓ |
| 2 | REFACTOR | 도메인 로직을 엔티티에 캡슐화 | — |

> 시니어 관점: TDD는 "테스트를 먼저 쓴다"가 아니라 "실패를 먼저 본다"가 핵심.
> RED를 확인하지 않으면 그 테스트가 무엇을 검증하는지 알 수 없다.

---

## 1. 검증한 규칙 (테스트가 강제하는 계약)

| ID | 규칙 | 이유 |
|---|---|---|
| R1 | 5종 이벤트 타입만 허용 | 임의 이벤트 주입 차단 |
| R2 | sessionId/formId/eventType 필수 | 집계 불가능 데이터 차단 |
| R3 | survey_started 세션당 1회(멱등) | **완료율 분모 정확성** — 중복 시작이 분모 부풀림 |
| R4 | abandoned는 started 이후·미제출만 | 제출 후/시작 전 이탈은 의미 없음 |
| R5 | question_answered가 last order 갱신 | 이탈 지점 분석 |
| R6 | submitted가 세션 종료 | 완료 집계 |
| R7 | 순서 역전 도착 시 last order는 MAX 유지 | 네트워크 지연으로 이벤트가 뒤바뀌어 와도 이탈 지점 정확 |

R3와 R7이 특히 중요하다. baseline의 정확성을 직접 좌우한다.

---

## 2. HTTP 계약

```
POST /api/events
Content-Type: application/json
(인증 불필요 — 익명 세션 허용)

{ "sessionId": "...", "formId": "<uuid>", "eventType": "survey_started",
  "questionId": null, "questionOrder": null, "experimentArm": null, "occurredAt": 1718... }

→ 202 Accepted   (수신 확인; 적재는 비동기)
→ 400 Bad Request { "error": "validation_failed", "fields": {...} }
```

### 왜 202인가
이벤트 로깅은 응답자 경험을 절대 지연시키면 안 된다. 수신만 확인(202)하고 적재는 @Async로
넘긴다. 200(처리 완료)이 아니라 202(수신됨)가 의미상 정확하다.

### 왜 인증 없이 허용하나
완료율의 분모는 비로그인 응답자도 포함해야 한다. anon_session_id로 추적하며 인증 인터셉터를
태우지 않는다. (단, rate limit과 formId 존재 검증은 필요 — 4장.)

---

## 3. 레이어 구조

```
EventController        @Valid로 1차 검증 → 202 / 400, service.ingest() 위임
  └ EventRequest       DTO + Bean Validation(@NotBlank, @Pattern 화이트리스트)
  └ GlobalExceptionHandler  검증 실패 → 400 + 사유
EventIngestService     @Async @Transactional. 세션 upsert + 전이 + 이벤트 append
  └ ResponseSession    엔티티가 전이 메서드 소유(markStarted/markSubmitted/...)
  └ SurveyEvent        append-only 원본
  └ Repositories       findByFormIdAndAnonSessionId 등
```

전이 규칙(applyTransition)은 순수 Java EventService.record와 1:1 동일 의미.

---

## 4. 프로덕션 전 보강 필요 (TODO)

TDD로 핵심 로직은 검증했으나, 운영에는 다음이 더 필요하다:
- **rate limit**: 익명 허용이므로 이벤트 폭주/남용 방지(IP·세션 단위).
- **formId 존재·활성 검증**: 없는 폼에 이벤트 적재 방지.
- **(form_id, anon_session_id) 동시 생성 경합**: 유니크 제약 + 충돌 시 재조회(현재 구조는
  유니크로 안전하나, 동시 최초 이벤트 2건이면 한쪽이 제약 위반 → 재시도 처리 추가).
- **payload 크기 제한**, **CORS**(응답 페이지 도메인), **sendBeacon Content-Type 허용**.
- **@Async 예외 처리**: 비동기 적재 실패 시 유실되지 않도록 큐/재시도 또는 DLQ.

---

## 5. 검증 상태 (정직한 한계)

- 핵심 로직(R1~R7): 순수 Java TDD로 실제 RED→GREEN 사이클 실행, 13 테스트 통과 ✓
- Spring 컨트롤러/서비스: 코드 작성 완료, 전이 규칙이 검증 로직과 동일함을 대조 확인 ✓
- MockMvc 계약 테스트(EventControllerTest)·Testcontainers 통합(EventIngestIT): **이 환경에서 미실행** ⚠️
  (Maven Central 차단). 로컬/CI에서 실행 필요. 테스트 코드는 작성되어 있음.

---

## 6. 산출물
| 파일 | 내용 |
|---|---|
| `tdd/EventLogicTest.java` | 핵심 규칙 R1~R6 테스트(순수 Java) |
| `tdd/EdgeCaseTest.java` | R7 경계 테스트 |
| `tdd/EventValidator.java` | 검증 로직 (GREEN 구현) |
| `tdd/EventService.java` | 전이 로직 (GREEN+REFACTOR 구현) |
| `spring/.../EventController.java` | REST 컨트롤러 |
| `spring/.../EventRequest.java` | DTO + Bean Validation |
| `spring/.../EventIngestService.java` | 적재 서비스(@Async) |
| `spring/.../ResponseSession.java` | 세션 엔티티(전이 캡슐화) |
| `spring/.../SurveyEvent.java` | 이벤트 엔티티 |
| `spring/.../GlobalExceptionHandler.java` | 400 변환 |
| `spring/.../Repositories.java` | JPA 리포지토리 |
| `spring/src/test/.../EventControllerTest.java` | MockMvc 계약 테스트 |
| `spring/src/test/.../EventIngestIT.java` | Testcontainers 통합 테스트 |
