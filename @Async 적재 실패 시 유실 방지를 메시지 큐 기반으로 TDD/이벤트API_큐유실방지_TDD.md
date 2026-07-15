# 품앗이폼 — 이벤트 적재 유실 방지: 메시지 큐 (TDD) v1.0

@Async 적재의 유실 위험을 메시지 큐로 대체. Red-Green-Refactor로 개발.

---

## 0. 문제: @Async는 왜 위험한가

기존 `ingest`는 `@Async`였다. 두 가지 유실 경로:
 1. 컨트롤러가 202를 반환한 뒤 비동기 처리 시작 전 서버 크래시/배포 → 이벤트 증발.
 2. 비동기 적재 중 DB 오류 → 조용히 묻힘(아무도 모름).

큐로 대체하되, "큐만 붙이면 끝"이 아니다. 큐 자체도 유실 지점을 만든다:
 - publish 실패, 컨슈머 처리 중 죽음, at-least-once로 인한 중복.
 → 재시도 + 멱등 + DLQ + 수신보장 4가지가 함께 있어야 진짜 보장된다.

---

## 1. 설계 결정: at-least-once + 멱등 소비

분산 시스템에서 exactly-once는 사실상 불가능. 현실적 선택:
 - 전달은 at-least-once(최소 한 번) — 유실보다 중복을 택한다.
 - 소비는 멱등 — 중복이 와도 결과가 같게.
 다행히 EventIngestService는 이미 멱등(세션 전이 R3/R4 + 세션 경합 복구). 그대로 활용.

---

## 2. 검증한 속성 (TDD)

| ID | 속성 | 막는 유실 |
|---|---|---|
| Q1 | 정상 메시지 적재 + ack | 기본 동작 |
| Q2 | 일시 오류 → nack → 재시도 → 성공 | DB 일시 장애로 인한 유실 |
| Q3 | 한도 초과 → DLQ 격리, 메인 큐 안 막힘 | 독약 메시지가 큐 전체를 막는 것 |
| Q4 | 중복 전달도 멱등 소비로 1건만 | at-least-once 중복 적재 |
| Q5 | publish 성공 후에만 202, 실패 시 503 | 수신→큐 사이 유실(거짓 수신확인) |

Q5가 특히 중요: 202를 먼저 주고 뒤에서 publish하면(fire-and-forget) publish 실패가
조용히 유실된다. publish 성공을 확인하고 202를 줘야 "202 = 큐에 안전히 들어감"이 성립.

---

## 3. TDD 사이클 기록 (실제 실행)

| 사이클 | 단계 | 내용 | 결과 |
|---|---|---|---|
| 1 | RED | QueueConsumerTest(Q1~Q4) 작성 | 컴파일 실패 ✓ |
| 1 | GREEN | InMemoryBroker/Consumer/Store 구현 | 10 pass ✓ |
| 2 | RED | IngestPathTest(Q5 수신→큐 유실) 추가 | 컴파일 실패 ✓ |
| 2 | GREEN | IngestHandler: publish 성공 후 수신확인 | 2 pass, Q1~Q4 회귀 없음 ✓ |
| 2 | REFACTOR | Spring 반영 + @Async 제거 | 전체 36 테스트 통과 ✓ |

> 큐 의미론(at-least-once/ack/nack/재시도/DLQ)을 InMemoryBroker로 모사해
> Kafka/RabbitMQ 없이 컨슈머 로직을 실제 검증했다.

---

## 4. Spring 반영

### 흐름 변화
```
[이전] 컨트롤러 → @Async ingest()  (유실 위험)
[이후] 컨트롤러 → publisher.publish() → [큐] → 컨슈머 → ingest()  (동기, 예외 전파)
```

### 핵심 코드 변경
- `EventController`: `service.ingest()` 직접 호출 → `publisher.publish()`.
  publish 성공 시 202, 실패 시 503(Q5). rate limit·검증은 그대로 앞단.
- `EventIngestService`: **@Async 제거**. 컨슈머 스레드에서 동기 실행되어야 적재 실패
  예외가 브로커로 전파되어 재시도/DLQ가 작동한다. (이게 유실 방지의 핵심 메커니즘.)
- `EventQueueConsumer`: @RabbitListener(또는 @KafkaListener) 예시. 예외 던지면 nack.
- `EventPublisher`: 브로커 추상화 인터페이스. RabbitTemplate/KafkaTemplate 래퍼.

### 브로커 설정(인프라)에 둘 것
- 재시도 횟수(maxAttempts), 백오프 정책
- DLQ(dead-letter-exchange/topic) 바인딩
- 컨슈머 prefetch/concurrency
- (Kafka면) 오프셋 커밋은 처리 성공 후에만(at-least-once)

---

## 5. 검증 상태 (정직한 한계)
- 큐 의미론·유실 방지 로직(Q1~Q5): 실제 RED→GREEN 사이클, 12 테스트 통과 ✓
- 전체 회귀(핵심13 + 운영보강11 + 큐12 = 36): 전부 통과 ✓
- Spring 반영(컨트롤러 큐화, @Async 제거, 컨슈머): 코드 작성·로직 대조 ✓, 빌드 미실행 ⚠️
- 실제 브로커(RabbitMQ/Kafka) 연동·DLQ 동작: 코드/설정 예시만, 통합 환경에서 검증 필요 ⚠️

---

## 6. 산출물
| 파일 | 내용 |
|---|---|
| `tdd3/QueueConsumerTest.java` | Q1~Q4 (재시도/DLQ/멱등) 테스트 |
| `tdd3/IngestPathTest.java` | Q5 (수신→큐 유실) 테스트 |
| `tdd3/InMemoryBroker.java` | 큐 의미론 모사(재시도/DLQ) |
| `tdd3/EventConsumer.java` | 멱등 소비 컨슈머(순수) |
| `tdd3/IngestHandler.java` | publish-then-confirm(순수) |
| `tdd3/FakeEventStore.java` | 실패 주입 스토어 |
| `spring/.../queue/EventQueueConsumer.java` | Spring 컨슈머(@RabbitListener 예시) |
| `spring/.../queue/EventPublisher.java` | 퍼블리셔 추상화 |
| `spring/.../queue/EventMessage.java` | 큐 메시지 |
| `spring/.../EventController.java` | 큐 publish 기반(503 처리) |
| `spring/.../EventIngestService.java` | @Async 제거, 컨슈머가 호출 |
