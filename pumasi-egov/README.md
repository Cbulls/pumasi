# 품앗이폼 eGov Walking Skeleton

> 제품 목표·전체 기능·개발 중 문제와 해결의 **장문 개요**는 저장소 루트 [README.md](../README.md)를 보세요.  
> 이 문서는 **백엔드 실행·계층·curl E2E·API 표**에 집중합니다.

품앗이폼(설문 품앗이 플랫폼)의 핵심 흐름을 **전자정부표준프레임워크(eGovFrame) + MyBatis + PostgreSQL**
위에서 실제로 빌드·실행되는 형태로 조립한 walking skeleton입니다.

관통하는 흐름 하나를 끝까지 동작시키는 것이 목표입니다:

```
폼 생성 → 질문 추가 → 게시(escrow 예치) → 응답 제출 → 크레딧 정산 → 결과 조회
```

## 아키텍처

전자정부표준프레임워크 계층형 구조를 따릅니다.

```
egovframework.pmsi
├── PumasiApplication                 Spring Boot 진입점
├── config/EgovConfigAppMapper        MyBatis: "sqlSession"(SqlSessionFactory) 빈 + 매퍼 XML 로딩
├── config/EgovConfigCommon           표준 공통: leaveaTrace 등 trace 빈(EgovAbstractServiceImpl 필수)
├── cmm                               공통(전역 예외 핸들러, 업무 예외, 보안 필터)
├── cmm.storage                       StorageClient 추상화(local 디스크 / S3 호환)
├── auth.{web,service,service.impl}   로그인 토큰(Bearer) 발급·검증
├── form.{web,service,service.impl}   폼 빌더·피드·공유·미디어 (+ 순수 FormValidator)
├── credit.{web,service,service.impl} 크레딧 정산·충전 (+ 순수 SettlementCalc)
├── response.{web,service,service.impl} 응답 수집·HOLD 검토 (+ 순수 QualityJudge)
├── event.{web,service.impl}          응답 퍼널 이벤트(view/start/submit) 수집·집계
└── result.{web,service,service.impl}   결과 집계·CSV·비동기 export (+ 순수 ResultAggregator)
```

표준 준수 지점:
- 서비스: 인터페이스 + `ServiceImpl extends EgovAbstractServiceImpl`
- DAO: `EgovAbstractMapper` 상속 + `resources/egovframework/mapper/pmsi/**/*.xml`
- DI: `@Resource(name=...)` 이름 기반 주입
- 비즈니스 로직(검증·판정·정산계산·집계)은 순수 Java로 분리 → 프레임워크와 무관하게 재사용/테스트

## 핵심 설계 결정 (스켈레톤 한정)

| 결정 | 내용 |
|---|---|
| 인증 | 로그인 토큰(`Authorization: Bearer`)만 신뢰. 비밀번호 없는 계정 선택 로그인은 `pmsi.auth.demo-enabled=true`일 때만 허용(개발/시연 전용, 프로덕션은 `PMSI_DEMO_AUTH=false`) |
| 크레딧 게이트(D7) | 게시 시 `cost × maxResponses` 를 escrow로 예치. pass 응답이 상한 도달 시 자동 마감(CLOSED), 수동 마감 시 미소진 escrow 환불 |
| 비용 산정(D4) | 질문 소요시간 근사(장문형 2분/그 외 1분), 최소 1크레딧 |
| 지급/소각(D5) | reward = floor(cost×0.8), burn = cost − reward |
| 동시성(D6) | 제작자 escrow 차감 = 비관적 락(`SELECT ... FOR UPDATE`), 응답자 적립 = 원자적 UPSERT, 응답 상한 검사 = 폼 행 잠금 |
| 멱등 | `credit_ledger(reason, ref_id)` UNIQUE (이중 정산·이중 환불 차단) |
| 품질 분리(§4.4) | reject도 데이터는 저장, 크레딧만 미지급. 집계(§4.5)는 pass만 |
| 소요시간 | 서버 측정: 응답 시작(`POST .../responses/start`)을 기록하고 제출 시 서버가 계산(클라이언트 값 미신뢰) |
| 답변 검증 | 필수 여부 + 유형별 값 검증(`AnswerValidator`): 보기 소속, 선택 개수, 글자수/정규식, 척도 범위, 날짜 형식 |
| 주의 문항 | RADIO에 `attentionAnswer` 지정 가능. 제출 답이 다르면 즉시 reject(성의 없는 응답 필터) |
| HOLD 검토 | hold 응답을 소유자가 `POST .../responses/{id}/review`로 pass(소급 정산, 멱등)/reject 전환 |
| 가드레일 | 최근 10건 중 reject 비율 60% 이상이면 폼 자동 `PAUSED`. 소유자가 `POST /pmsi/form/{id}/resume`으로 재개 |
| 스토리지 | `StorageClient` 추상화 — `pmsi.storage.mode=local`(기본, `./uploads`) 또는 `s3`(S3 호환, MinIO 포함) |
| 크레딧 충전 | 베타용 Fake 충전 API(`pmsi.credit.fake-purchase-enabled`). 멱등 키로 이중 충전 차단. 실PG는 후속 |

## 지원 질문 유형

응답형 8종: `SHORT_TEXT`, `LONG_TEXT`, `RADIO`, `CHECKBOX`, `DROPDOWN`, `LINEAR_SCALE`, `RATING`, `DATE`  
콘텐츠 블록(응답·비용 제외): `DESCRIPTION`, `IMAGE` · 첨부: `FILE`

## 사전 요구사항

- JDK 21
- Gradle 8.x (또는 IntelliJ의 내장 Gradle) — 최초 1회 `gradle wrapper`로 래퍼 생성 권장
- Docker (PostgreSQL 실행용) 또는 로컬 PostgreSQL 16
- 네트워크: `mavenCentral()` + `https://maven.egovframe.go.kr/maven/` (표준프레임워크 rte 의존성)

## 실행

### 1) DB 기동

```bash
docker compose up -d          # localhost:5432, db=pumasi user/pw=pumasi
```

### 2) 애플리케이션 실행

```bash
gradle bootRun                # Flyway가 V1~V12 마이그레이션 + seed 자동 적용
```

seed 계정(초기 크레딧): `u-owner`(available 1000), `u-alice`(50), `u-bob`(50), `SYSTEM`(0).
인증: 모든 `/pmsi/**` 는 로그인 토큰(Authorization: Bearer) 필요(`/pmsi/auth/login` 제외).
헬스체크: `GET /actuator/health` (liveness/readiness probe 포함, 인증 불필요).

### 환경 변수 (프로덕션 배포 시)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PMSI_DB_URL` / `PMSI_DB_USER` / `PMSI_DB_PASSWORD` | localhost pumasi | DB 접속 (평문 커밋 금지) |
| `PMSI_DEMO_AUTH` | `true` | `false`면 비밀번호 없는 데모 로그인 차단 — 프로덕션 필수 |
| `PMSI_CORS_ORIGINS` | `http://localhost:3000` | 콤마 구분 허용 오리진 |
| `PMSI_STORAGE_MODE` | `local` | `s3`면 `PMSI_S3_BUCKET`/`PMSI_S3_REGION`/`PMSI_S3_ENDPOINT`(S3 호환용) 필요 |
| `PMSI_UPLOAD_DIR` | `./uploads` | local 모드 업로드 루트 |
| `PMSI_FAKE_PURCHASE` | `true` | `false`면 Fake 크레딧 충전 API 차단(실PG 전환 시) |

## End-to-End 시나리오 (curl)

```bash
BASE=http://localhost:8080

# 0) 로그인해서 토큰 발급 (X-User-Id 신뢰 제거됨)
OWNER=$(curl -s -X POST $BASE/pmsi/auth/login -H 'Content-Type: application/json' \
  -d '{"userId":"u-owner"}' | sed 's/.*"token":"//; s/".*//')
ALICE=$(curl -s -X POST $BASE/pmsi/auth/login -H 'Content-Type: application/json' \
  -d '{"userId":"u-alice"}' | sed 's/.*"token":"//; s/".*//')
BOB=$(curl -s -X POST $BASE/pmsi/auth/login -H 'Content-Type: application/json' \
  -d '{"userId":"u-bob"}' | sed 's/.*"token":"//; s/".*//')

# 1) 폼 생성 (owner = 토큰 주체)
FORM=$(curl -s -X POST $BASE/pmsi/form \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $OWNER" \
  -d '{"title":"색깔 설문","description":"데모","maxResponses":5}' | sed 's/.*"formId":"//; s/".*//')
echo "formId=$FORM"

# 2) 질문 추가
curl -s -X POST $BASE/pmsi/form/$FORM/questions -H 'Content-Type: application/json' -H "Authorization: Bearer $OWNER" \
  -d '{"type":"RADIO","title":"가장 좋아하는 색은?","required":true,"options":["빨강","파랑","초록"]}'
curl -s -X POST $BASE/pmsi/form/$FORM/questions -H 'Content-Type: application/json' -H "Authorization: Bearer $OWNER" \
  -d '{"type":"LINEAR_SCALE","title":"만족도","required":true,"scaleMin":1,"scaleMax":5}'
curl -s -X POST $BASE/pmsi/form/$FORM/questions -H 'Content-Type: application/json' -H "Authorization: Bearer $OWNER" \
  -d '{"type":"SHORT_TEXT","title":"한줄 평","required":false}'

# 3) 게시 → escrow 예치
curl -s -X POST $BASE/pmsi/form/$FORM/publish -H "Authorization: Bearer $OWNER"
curl -s $BASE/pmsi/credit/me -H "Authorization: Bearer $OWNER"   # available 985, escrow 15

# 질문 ID 확인
curl -s $BASE/pmsi/form/$FORM/questions -H "Authorization: Bearer $OWNER"

# 4-a) u-alice 정상 응답 → pass (동의 필수: consentAgreed=true)
#      먼저 응답 시작을 신고(start) — 소요시간은 서버가 측정한다
curl -s -X POST $BASE/pmsi/form/$FORM/responses/start -H "Authorization: Bearer $ALICE"
sleep 10   # 문항 수 × 2초 이상 지나야 pass (고속 제출은 reject)
curl -s -X POST $BASE/pmsi/form/$FORM/responses -H 'Content-Type: application/json' -H "Authorization: Bearer $ALICE" \
  -d '{"consentAgreed":true,"answers":[
        {"questionId":"<RADIO_Q_ID>","values":["파랑"]},
        {"questionId":"<SCALE_Q_ID>","values":["4"]},
        {"questionId":"<TEXT_Q_ID>","values":["좋아요"]}]}'
# 응답의 anonLabel(익명-xxxxxx)로 응답자 식별자가 감춰짐

# 4-b) u-bob 고속 제출 → reject (start 직후 바로 제출하면 서버 측정 소요시간이 부족)
curl -s -X POST $BASE/pmsi/form/$FORM/responses/start -H "Authorization: Bearer $BOB"
curl -s -X POST $BASE/pmsi/form/$FORM/responses -H 'Content-Type: application/json' -H "Authorization: Bearer $BOB" \
  -d '{"consentAgreed":true,"answers":[{"questionId":"<RADIO_Q_ID>","values":["빨강"]}]}'

# 5) 결과 조회 (소유자 토큰 필요, 비소유자는 403)
curl -s $BASE/pmsi/form/$FORM/results -H "Authorization: Bearer $OWNER"

# 6) 본인 잔액 확인 (/me)
curl -s $BASE/pmsi/credit/me -H "Authorization: Bearer $ALICE"
curl -s $BASE/pmsi/credit/me -H "Authorization: Bearer $OWNER"

# 7) 수동 마감 — 미소진 escrow가 available로 환불된다
curl -s -X POST $BASE/pmsi/form/$FORM/close -H "Authorization: Bearer $OWNER"
curl -s $BASE/pmsi/credit/me -H "Authorization: Bearer $OWNER"

# 인증/동의/한도 실패 예시
curl -s -o /dev/null -w '%{http_code}\n' $BASE/pmsi/feed                       # 401 (토큰 없음)
```

## 테스트

```bash
gradle test                                   # 순수 로직 단위 테스트(무DB) — 항상 실행
PUMASI_IT=true gradle test --tests '*IT'      # 정산 멱등 통합 테스트(DB 필요) — 그 외엔 skip
```

- `SettlementCalcTest`, `QualityJudgeTest`, `AnswerValidatorTest`, `FormValidatorTest`, `ResultAggregatorTest`, `ImageAssetServiceTest`: DB 없이 도는 순수 로직 검증(신규 유형 DROPDOWN/RATING/DATE·주의 문항 포함).
- `CreditSettlementIdempotencyIT`: 같은 responseId 2회 정산 → 1회만 반영(멱등) 확인. `PUMASI_IT=true` + DB일 때만 실행.
- CI: [.github/workflows/ci.yml](../.github/workflows/ci.yml) — push/PR마다 `gradle test` + web `npm run build`.

## 데이터 모델 (Flyway)

| 버전 | 테이블 |
|---|---|
| V1 | form, form_section, form_question, form_question_option |
| V2 | credit_balance, credit_ledger (+ seed) |
| V3 | survey_response, survey_answer |
| V4 | survey_response에 anon_label, consent_at 추가(익명화·동의) |
| V5 | user_account(seed), auth_session(로그인 토큰) |
| V6 | response_session(응답 시작 시각 — 서버 소요시간 측정) |
| V7 | form.closes_at(마감 시각) |
| V8 | form_question.body_html, image_url(문항 미디어) |
| V9 | branch_rules(조건부 분기), share_token(공개 공유) |
| V10 | 무결성 강화: FK, CHECK, SYSTEM 유저, 인덱스, ledger GENESIS 백필 |
| V11 | image_url 에셋 규약 주석(`?v=thumb\|display\|orig`) |
| V12 | attention_answer(주의 문항), form.status에 PAUSED(가드레일), survey_event(퍼널), export_job(비동기 export), 응답자 인덱스 |

## 범위 밖 (현 시점)

실인증(OAuth/매직링크 — 데모 로그인 게이트만 존재), 실제 PG 연동(Fake 충전으로 대체),
POI xlsx(CSV로 대체), 하이브리드 랭킹 점수식 전체(현재는 상호 부스트+채움률 정렬),
그리드·퀴즈 등 나머지 질문 유형, Redis 분산 rate limit, Sentry 등 에러 알림.

## API 요약

모든 경로는 `Authorization: Bearer <token>` 필요(로그인 제외).

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/pmsi/auth/login` | 로그인(계정 선택) → 토큰 발급 (인증 불필요) |
| GET | `/pmsi/auth/me` | 토큰 유효성 확인 → { userId } (무효 시 401) |
| POST | `/pmsi/auth/logout` | 로그아웃(세션 무효화) |
| POST | `/pmsi/form` | 폼 생성 (owner=토큰 주체) |
| POST | `/pmsi/form/{id}/questions` | 질문 추가 (소유자만) |
| PUT/DELETE | `/pmsi/form/{id}/questions/{qid}` | 질문 수정·삭제 / `POST .../questions/reorder` 순서 변경 |
| POST | `/pmsi/form/{id}/sections` | 섹션 추가 (분기 규칙 포함) |
| POST | `/pmsi/form/{id}/publish` | 게시 + escrow 예치 (소유자만) |
| POST | `/pmsi/form/{id}/close` | 마감 + 미소진 escrow 환불 (소유자만) |
| POST | `/pmsi/form/{id}/resume` | 가드레일 PAUSED 폼 재개 (소유자만) |
| GET | `/pmsi/form/{id}` | 폼 조회 |
| GET | `/pmsi/form/{id}/questions` | 질문 목록 |
| GET | `/pmsi/form` | 내 폼 목록(토큰 주체 기준) |
| GET | `/pmsi/feed?page&size` | 응답 피드(게시 중 + 남의 설문 + 미응답 + escrow 잔여, 상호 부스트·채움률 정렬, 페이지네이션) |
| POST | `/pmsi/form/{id}/media` | 문항 이미지 업로드(WebP thumb/display/orig 파생) |
| GET | `/pmsi/form/{id}/media/{assetId}?v=` | 문항 이미지 서빙 |
| POST | `/pmsi/form/{id}/files` / GET `.../files/{fileId}` | 응답 FILE 첨부 업로드·다운로드 |
| GET | `/pmsi/public/forms/{shareToken}` | 공개 공유 미리보기 (인증 불필요) |
| POST | `/pmsi/form/{id}/responses/start` | 응답 시작 신고(서버 소요시간 측정 시작) |
| POST | `/pmsi/form/{id}/responses` | 응답 제출(동의 필수, 익명화, 정원 초과 시 409) |
| POST | `/pmsi/form/{id}/responses/{rid}/review` | hold 응답 검토: pass(소급 정산)/reject (소유자만) |
| GET | `/pmsi/form/{id}/results` | 결과 집계(pass + 상호 언락된 응답만, 소유자만) |
| GET | `/pmsi/form/{id}/results/responses` | 언락된 개별 응답 목록 |
| GET | `/pmsi/form/{id}/results/export.csv` | 동기 CSV (언락분만) |
| POST | `/pmsi/form/{id}/results/export-jobs` | 비동기 export job 생성 → GET `.../{jobId}` 상태 → GET `.../{jobId}/download` |
| POST | `/pmsi/events` | 퍼널 이벤트 기록(view/start/submit) |
| GET | `/pmsi/form/{id}/events/funnel` | 퍼널 집계(소유자만) |
| GET | `/pmsi/credit/me` | 본인 크레딧 잔액 |
| POST | `/pmsi/credit/purchase` | Fake 크레딧 충전(멱등 키, 베타 전용 플래그) |
| GET | `/actuator/health` | 헬스체크 (인증 불필요) |

## 보안·개인정보보호 방어
- 인증: 로그인 토큰(Bearer)만 신뢰. `AuthInterceptor`가 미인증 요청 401. `X-User-Id` 신뢰 제거.
- 인가: 질문 추가·게시·마감·결과 조회는 소유자만. 폼 목록은 토큰 주체 기준(IDOR 방지).
- 응답 익명화: 결과/응답에 `anon_label`만 노출, 실제 식별자는 내부용.
- 동의: 응답 제출 시 `consentAgreed` 필수(미동의 400).
- Rate limit: 쓰기 요청 인메모리 토큰버킷(초과 429). 인증 경로는 IP 기준(재로그인 우회 차단).
- 표면 축소: 보안 응답 헤더, JSON 요청 256KB 제한(413; multipart 업로드 `/files`·`/media` POST는 multipart 상한 8MB로 별도 제어), 500 응답 내부 메시지 미노출, PII 로그 억제.
- 입력 검증: Bean Validation(@Valid) + 유형별 FormValidator/AnswerValidator/QualityJudge.
- 어뷰징: 소요시간 서버 측정(start→submit), 주의 문항 오답 즉시 reject, pass 상한(maxResponses) 도달 시 자동 마감, reject 급증 시 가드레일 자동 PAUSED.
- 환경 분리: 데모 로그인(`PMSI_DEMO_AUTH`)·CORS 오리진(`PMSI_CORS_ORIGINS`)·DB 자격증명 전부 env로 주입, 코드에 하드코딩 없음.
