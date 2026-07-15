# 품앗이폼 — 이벤트 API 운영 보강 (TDD) v1.0

이벤트 수신 API의 프로덕션 보강. 핵심은 세션 생성 경합(크레딧 동시성과 같은 뿌리, 다른 해법).

---

## 1. 세션 생성 경합 — 가장 중요

### 진단 (재현 검증)
현재 `ingest`의 find-or-create는 동시 최초 이벤트에서 깨진다. 같은 (formId, sessionId)로
`survey_viewed`와 `survey_started`가 거의 동시에 다른 스레드에서 처리되면, 둘 다 "세션 없음"을
보고 둘 다 INSERT → 유니크 제약 위반으로 **한쪽이 통째로 실패**. 그 이벤트가 `survey_started`면
완료율 분모가 누락된다 = 측정 인프라 오염.

`SessionRaceTest`로 재현: race window를 실제 DB 왕복만큼 벌리면 64개 동시 요청 중
2~4건이 UniqueViolation으로 처리 실패(= 이벤트 유실).

> TDD 교훈: 처음엔 인메모리 putIfAbsent가 너무 빨라 경합이 재현 안 됨(거짓 음성).
> "통과했으니 됐다"가 아니라 "내 테스트가 실제를 반영하나?"를 의심하고 race window를
> 실제 DB처럼 모사해서야 NAIVE가 진짜 깨지는 걸 확인했다.

### 해법: insert-first, recover-on-conflict
```
1) 조회 (빠른 경로, 대부분 이미 존재)
2) 없으면 saveAndFlush 시도 (flush로 제약 위반 즉시 표면화)
3) DataIntegrityViolation 잡으면 = 동시에 누가 먼저 생성 → 재조회(이긴 쪽 행 반드시 존재)
```
`SafeResolver`로 검증: 같은 race window에서 처리 실패 0, 세션 정확히 1개, 모두 같은 ID.

### 크레딧 동시성과의 대비 (핵심 통찰)
| | 경쟁 성격 | 해법 |
|---|---|---|
| 크레딧 예치금 차감 | 같은 행에 반복 경쟁(핫 계정) | 비관적 락(FOR UPDATE) |
| 세션 생성 | 생애 1회뿐인 생성 경쟁 | 낙관적 insert-recover |

같은 "동시성" 문제라도 경쟁의 성격이 다르면 해법이 다르다. 세션 생성에 비관적 락을 쓰면
매 이벤트마다 불필요한 락 비용을 치른다.

---

## 2. Rate limit (익명 엔드포인트 보호)

익명 허용이라 이벤트 폭주에 무방비. 토큰 버킷으로 세션 키별 제한.

- `RateLimiterTest`로 TDD: 용량까지 허용/초과 거부/시간경과 보충/키별 독립 (7 테스트).
- 시간을 LongSupplier로 주입 → "2초 후 2개 보충" 같은 시간 로직을 결정론적으로 검증.
- 컨트롤러: 초과 시 429 Too Many Requests, 적재 안 함.
- 기본값 capacity 60 / 초당 2개 (정상 응답 속도는 허용, 자동화 폭주 차단). 설정으로 분리.
- 분산 배포 시 인메모리 → Redis 토큰버킷 교체(tryAcquire 인터페이스 유지).

---

## 3. 남은 운영 항목 (설계 방향)

### 3-1. @Async 적재 실패 시 유실 방지
비동기 적재가 실패하면 이벤트가 조용히 사라진다. 선택지:
- (a) 수신 즉시 메시지 큐(Kafka/RabbitMQ)에 넣고 컨슈머가 적재 — 큐가 버퍼+재시도+DLQ 제공.
      이벤트량이 많은 측정 인프라엔 이 방식이 정석.
- (b) @Async + @Retryable + 실패 시 dead-letter 테이블 기록.
- 권장: 초기엔 (b)로 시작, 트래픽 증가 시 (a)로 전환. 어느 쪽이든 "실패가 조용히 묻히지 않게"가 원칙.

### 3-2. formId 존재·활성 검증
없는/마감된 폼에 이벤트 적재 방지. 단, 매 이벤트마다 폼 조회는 비용 → 폼 ID를 캐시(Redis/로컬)에
두고 검증. 캐시 미스 시에만 DB 확인.

### 3-3. 기타
- payload 크기 제한(Spring `spring.servlet.multipart` / 서버 max body size)
- CORS: 응답 페이지 도메인만 허용
- sendBeacon의 Content-Type(text/plain일 수 있음) 허용 처리
- occurredAt(클라이언트 시각) 신뢰 금지 — 서버 수신 시각을 권위 있는 값으로(시계 조작 방지)

---

## 4. 테스트 요약 (전부 실행 검증)

| 테스트 | 대상 | 결과 |
|---|---|---|
| EventLogicTest | 검증·멱등·전이 R1~R6 | 12 pass |
| EdgeCaseTest | 순서 역전 R7 | 1 pass |
| SessionRaceTest | 세션 생성 경합 (NAIVE 깨짐/SAFE 복구) | 4 pass |
| RateLimiterTest | 토큰 버킷 L1~L4 | 7 pass |
| **합계** | | **24 pass** |

MockMvc 계약 테스트(429 포함)·Testcontainers 통합은 코드 작성됨, 로컬/CI 실행 필요.

---

## 5. 검증 상태 (정직한 한계)
- 순수 로직(세션경합 복구, rate limit): 실제 RED→GREEN 사이클 + 동시성 재현으로 검증 ✓
- Spring 반영(resolveSession insert-recover, 컨트롤러 429): 코드 작성·로직 대조 ✓, 빌드 미실행 ⚠️
- @Async 유실 방지·formId 검증: 설계 방향만 제시(구현은 다음 사이클) ⚠️
