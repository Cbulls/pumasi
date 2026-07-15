# 품앗이폼 — 대용량 비동기 내보내기 (TDD) v1.0

수만~수십만 건 응답을 OOM/타임아웃 없이 내보내기. Red-Green-Refactor로 개발.

---

## 0. 왜 동기 다운로드로 안 되는가

기존 동기 `exportXlsx`는 전체를 메모리에 올려 한 번에 만들어 반환한다. 대용량이면 셋이 터진다:
 1. 메모리 — 전체 행렬을 힙에 올리면 OOM.
 2. 시간 — 생성에 수십 초면 HTTP 타임아웃.
 3. 동시성 — 여러 명이 큰 파일 동시 요청 시 서버 마비.

→ 요청과 다운로드를 분리. 요청 시 즉시 작업 ID 반환(202), 백그라운드 생성, 폴링 후 다운로드.

---

## 1. 작업 생애주기 (ExportJobTest, 15 테스트)

비동기 내보내기는 본질적으로 상태 기계.
```
PENDING → RUNNING → COMPLETED   (정상)
                  → FAILED      (실패)
COMPLETED/FAILED = 종료 상태(전이 불가)
```

| ID | 규칙 |
|---|---|
| J1 | 작업 생성 시 PENDING + 고유 jobId |
| J2 | PENDING→RUNNING→COMPLETED, 또는 RUNNING→FAILED |
| J3 | ★중복 합치기★ 같은 (form,user,format,expand) 진행 중 작업 있으면 재사용 |
| J4 | COMPLETED는 다운로드 URL + 만료시각 |
| J5 | ★잘못된 전이 거부★ 종료 상태에서 못 나감 |
| J6 | FAILED는 사유 보유, 재요청은 새 작업(기존이 종료 상태) |
| J7 | ★만료★ TTL 지나면 다운로드 불가 |

> J3+J6를 DB로 옮기는 법(REFACTOR): 부분 유니크 인덱스
> `WHERE status IN ('PENDING','RUNNING')` — 진행 중은 하나만, 종료된 건 여러 개 허용.

> 디버깅 사례: J7 만료 테스트가 처음 실패. 원인은 markCompleted가 시스템 시계를 박아서
> 테스트의 가상 시각(now=1초)과 어긋남. 완료 시각을 주입 가능하게 바꿔 해결
> (rate limiter에서 시간 주입한 것과 같은 교훈 — 시간 의존은 테스트 결정론을 깬다).

## 2. 스트리밍 생성 (StreamingExportTest, 8 테스트)

전체를 메모리에 안 올리고 청크 단위로 흘려보낸다.

| ID | 규칙 |
|---|---|
| S1 | 응답을 페이지(청크) 단위로 가져와 즉시 쓰고 버림 |
| S2 | ★메모리 상한★ 가장 큰 청크도 pageSize 이하 |
| S3 | 헤더는 한 번만 |
| S4 | 청크 경계에서 누락 없음(전체 출력) |
| S5 | 빈 결과도 헤더는 출력 |
| S6 | 진행률 추적(처리량/전체) |

> 메모리 원리: 페이저에서 한 페이지(≤pageSize)를 받아 싱크에 쓰고 버린다. 다음 페이지를
> 받을 때 이전 페이지는 GC 대상. → 메모리는 한 페이지 분량으로 상한.

---

## 3. Spring 구현 (REFACTOR)

### API
```
POST /api/forms/{id}/results/export-jobs        → 202 + jobId (중복은 기존 jobId)
GET  /api/forms/{id}/results/export-jobs/{jid}  → 상태(+ 완료 시 다운로드 URL)
```

### 흐름
```
requestExport → jobStore.findOrCreate(중복 합치기) → @Async generate() → 즉시 jobId 반환
generate(): markRunning → StreamingExporter로 저장소에 스트리밍 업로드
            → markCompleted(URL) / 실패 시 markFailed(사유)
클라이언트: 상태 폴링 → COMPLETED면 다운로드 URL 사용
```

### 핵심 설계
- **@Async 자기호출**: requestExport가 같은 빈의 generate()를 호출하면 프록시를 안 거쳐
  비동기가 안 된다. @Lazy 자기 주입(self)으로 프록시 경유(`self.generate()`).
- **스트리밍 싱크**: 저장소(S3) 멀티파트 업로드로 흘려보냄. SXSSFWorkbook(xlsx) 또는
  BufferedWriter(csv). 한 페이지씩 flush → 메모리 상한.
- **실패 노출**: 생성 실패는 markFailed로 상태에 기록. 조용히 묻지 않음
  (큐 유실방지와 같은 원칙 — 실패가 보여야 재요청 가능).
- **작업 상태 공유**: 여러 인스턴스가 공유하도록 DB(export_job) 또는 Redis.
  인메모리 ExportJobManager는 단일 인스턴스 검증용.
- **만료 정리**: completed_at + TTL 지난 작업·저장소 파일을 @Scheduled 배치로 삭제.
- **페이지네이션**: keyset 권장(OFFSET은 깊어지면 느림).

### 포트(의존 역전)
- `ExportJobStore`: 작업 상태(ExportJobManager의 DB판)
- `StoragePort` / `StorageStreamingSink`: 저장소 스트리밍 업로드
- `ResponsePagerFactory` / `ResponsePager`: 폼별 응답 페이지네이션

---

## 4. 검증 상태 (정직한 한계)
- 작업 생애주기·스트리밍 로직: 순수 Java로 실제 RED→GREEN, 23 테스트 통과 ✅
- Spring 컨트롤러/서비스/포트: 코드 작성·로직 대조 ✅, 빌드 미실행 ⚠️
- StreamingExporter가 FakeResponsePager 타입에 결합됨 — Spring에선 ResponsePager
  인터페이스로 일반화 필요(REFACTOR 시 시그니처 교체) ⚠️
- 포트 구현체(S3 멀티파트, DB jobStore, keyset 페이저): 미구현 ⬜
- @Async 자기호출 프록시·SXSSFWorkbook 실제 스트리밍: 로컬 검증 필요 ⚠️
- 미세 보강 TODO:
  - 만료 정리 @Scheduled 배치
  - 동기 임계(예: 5천 건) 초과 시 비동기 전환 안내(413 또는 프론트 분기)
  - 진행률을 폴링 응답에 노출(processed/total %)

---

## 5. 산출물
| 파일 | 내용 |
|---|---|
| `tdd/ExportJobTest.java` | 작업 생애주기 15 테스트 |
| `tdd/ExportJobManager.java` | 상태 기계 + 정책 |
| `tdd/StreamingExportTest.java` | 스트리밍 8 테스트 |
| `tdd/StreamingExporter.java` | 청크 스트리밍 |
| `spring/.../ExportJobController.java` | 요청·폴링 API |
| `spring/.../AsyncExportService.java` | @Async 실행 조율 |
| `spring/.../ports.java` | 포트 인터페이스 모음 |
| `spring/.../db/migration/V5__export_job.sql` | 작업 테이블 + 부분 유니크 인덱스 |
