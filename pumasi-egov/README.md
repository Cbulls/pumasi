# 품앗이폼 eGov Walking Skeleton

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
├── cmm                               공통(전역 예외 핸들러, 업무 예외)
├── form.{web,service,service.impl}   폼 빌더 (+ 순수 FormValidator)
├── credit.{web,service,service.impl} 크레딧 정산 (+ 순수 SettlementCalc)
├── response.{web,service,service.impl} 응답 수집 (+ 순수 QualityJudge)
└── result.{web,service,service.impl}   결과 집계 (+ 순수 ResultAggregator)
```

표준 준수 지점:
- 서비스: 인터페이스 + `ServiceImpl extends EgovAbstractServiceImpl`
- DAO: `EgovAbstractMapper` 상속 + `resources/egovframework/mapper/pmsi/**/*.xml`
- DI: `@Resource(name=...)` 이름 기반 주입
- 비즈니스 로직(검증·판정·정산계산·집계)은 순수 Java로 분리 → 프레임워크와 무관하게 재사용/테스트

## 핵심 설계 결정 (스켈레톤 한정)

| 결정 | 내용 |
|---|---|
| 인증 | `X-User-Id` 헤더 스텁(소셜 로그인 미구현). seed 유저로 시연 |
| 크레딧 게이트(D7) | 게시 시 `cost × maxResponses` 를 escrow로 예치 |
| 비용 산정(D4) | 질문 소요시간 근사(장문형 2분/그 외 1분), 최소 1크레딧 |
| 지급/소각(D5) | reward = floor(cost×0.8), burn = cost − reward |
| 동시성(D6) | 제작자 escrow 차감 = 비관적 락(`SELECT ... FOR UPDATE`), 응답자 적립 = 원자적 UPSERT |
| 멱등 | `credit_ledger(reason, ref_id)` UNIQUE (이중 정산 차단) |
| 품질 분리(§4.4) | reject도 데이터는 저장, 크레딧만 미지급. 집계(§4.5)는 pass만 |

## 지원 질문 유형(스켈레톤)

`SHORT_TEXT`, `LONG_TEXT`, `RADIO`, `CHECKBOX`, `LINEAR_SCALE`

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
gradle bootRun                # Flyway가 V1~V5 마이그레이션 + seed 자동 적용
```

seed 계정(초기 크레딧): `u-owner`(available 1000), `u-alice`(50), `u-bob`(50), `SYSTEM`(0).
인증: 모든 `/pmsi/**` 는 로그인 토큰(Authorization: Bearer) 필요(`/pmsi/auth/login` 제외).

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
curl -s -X POST $BASE/pmsi/form/$FORM/responses -H 'Content-Type: application/json' -H "Authorization: Bearer $ALICE" \
  -d '{"elapsedSeconds":40,"consentAgreed":true,"answers":[
        {"questionId":"<RADIO_Q_ID>","values":["파랑"]},
        {"questionId":"<SCALE_Q_ID>","values":["4"]},
        {"questionId":"<TEXT_Q_ID>","values":["좋아요"]}]}'
# 응답의 anonLabel(익명-xxxxxx)로 응답자 식별자가 감춰짐

# 4-b) u-bob 고속 제출 → reject
curl -s -X POST $BASE/pmsi/form/$FORM/responses -H 'Content-Type: application/json' -H "Authorization: Bearer $BOB" \
  -d '{"elapsedSeconds":1,"consentAgreed":true,"answers":[{"questionId":"<RADIO_Q_ID>","values":["빨강"]}]}'

# 5) 결과 조회 (소유자 토큰 필요, 비소유자는 403)
curl -s $BASE/pmsi/form/$FORM/results -H "Authorization: Bearer $OWNER"

# 6) 본인 잔액 확인 (/me)
curl -s $BASE/pmsi/credit/me -H "Authorization: Bearer $ALICE"
curl -s $BASE/pmsi/credit/me -H "Authorization: Bearer $OWNER"

# 인증/동의/한도 실패 예시
curl -s -o /dev/null -w '%{http_code}\n' $BASE/pmsi/feed                       # 401 (토큰 없음)
```

## 테스트

```bash
gradle test                                   # 순수 로직 단위 테스트(무DB) — 항상 실행
PUMASI_IT=true gradle test --tests '*IT'      # 정산 멱등 통합 테스트(DB 필요) — 그 외엔 skip
```

- `SettlementCalcTest`, `QualityJudgeTest`: DB 없이 도는 순수 로직 검증.
- `CreditSettlementIdempotencyIT`: 같은 responseId 2회 정산 → 1회만 반영(멱등) 확인. `PUMASI_IT=true` + DB일 때만 실행.

## 데이터 모델 (Flyway)

| 버전 | 테이블 |
|---|---|
| V1 | form, form_section, form_question, form_question_option |
| V2 | credit_balance, credit_ledger (+ seed) |
| V3 | survey_response, survey_answer |
| V4 | survey_response에 anon_label, consent_at 추가(익명화·동의) |
| V5 | user_account(seed), auth_session(로그인 토큰) |

## 스켈레톤 범위 밖

피드/매칭, 가드레일, 이벤트 측정 파이프라인, 비동기 엑셀(POI/S3), 실제 PG 연동,
조건부 분기, 12종 전체 질문 유형, Redis 분산 rate limit, 관측성.

## API 요약

모든 경로는 `Authorization: Bearer <token>` 필요(로그인 제외).

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/pmsi/auth/login` | 로그인(계정 선택) → 토큰 발급 (인증 불필요) |
| POST | `/pmsi/form` | 폼 생성 (owner=토큰 주체) |
| POST | `/pmsi/form/{id}/questions` | 질문 추가 |
| POST | `/pmsi/form/{id}/publish` | 게시 + escrow 예치 |
| GET | `/pmsi/form/{id}` | 폼 조회 |
| GET | `/pmsi/form/{id}/questions` | 질문 목록 |
| GET | `/pmsi/form?ownerId=` | 내 폼 목록 |
| GET | `/pmsi/feed` | 응답 피드(게시된 남의 설문) |
| POST | `/pmsi/form/{id}/responses` | 응답 제출(동의 필수, 익명화) |
| GET | `/pmsi/form/{id}/results` | 결과 집계(pass만, 소유자만) |
| GET | `/pmsi/credit/me` | 본인 크레딧 잔액 |

## 보안·개인정보보호 방어
- 인증: 로그인 토큰(Bearer)만 신뢰. `AuthInterceptor`가 미인증 요청 401. `X-User-Id` 신뢰 제거.
- 응답 익명화: 결과/응답에 `anon_label`만 노출, 실제 식별자는 내부용.
- 동의: 응답 제출 시 `consentAgreed` 필수(미동의 400).
- Rate limit: 쓰기 요청 인메모리 토큰버킷(초과 429).
- 표면 축소: 보안 응답 헤더, 요청 크기 제한(413), 500 응답 내부 메시지 미노출, PII 로그 억제.
- 입력 검증: Bean Validation(@Valid) + 유형별 FormValidator/QualityJudge.
