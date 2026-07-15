# 품앗이폼 프론트엔드 (pumasi-web)

품앗이폼 백엔드([../pumasi-egov](../pumasi-egov))의 REST API에 연결되는 **Next.js + TypeScript** 프론트엔드입니다.
폼 빌더 → 응답 피드/응답 → 크레딧 정산 → 결과 대시보드까지 4단계 흐름을 화면으로 관통합니다.

## 스택
- Next.js 14 (App Router) + TypeScript
- Tailwind CSS (모바일 우선)
- @tanstack/react-query (서버 상태)
- recharts (막대/원형/히스토그램)

## 화면
| 경로 | 설명 |
|---|---|
| `/` | 내 설문 대시보드(내 폼 목록, 상태 배지, CTA) |
| `/forms/new` | 폼 빌더(생성 → 질문 추가 → 게시/예치). `?formId=`로 초안 이어 편집 |
| `/feed` | 응답 피드(게시된 남의 설문, 보상 미리보기) |
| `/forms/[id]/respond` | 응답(유형별 입력, 진행률, 소요시간 측정 → 제출 결과) |
| `/forms/[id]/results` | 결과 대시보드(pass만 집계 차트, 비소유자는 블러 게이트) |

상단 헤더의 **사용자 스위처**로 데모 계정(`u-owner`/`u-alice`/`u-bob`)을 전환합니다. 선택값이 모든 요청의 `X-User-Id` 헤더가 됩니다(백엔드 인증 스텁). 크레딧 배지는 실시간 잔액을 보여줍니다.

## 사전 준비: 백엔드 실행
프론트는 백엔드가 떠 있어야 동작합니다. 이번 작업에서 백엔드에 **CORS 설정**과 **피드 엔드포인트(`GET /pmsi/feed`)**를 추가했으므로 백엔드를 다시 빌드/기동하세요.

```bash
cd ../pumasi-egov
docker compose up -d      # PostgreSQL
gradle bootRun            # http://localhost:8080
```

## 실행

```bash
cp .env.local.example .env.local   # NEXT_PUBLIC_API_BASE=http://localhost:8080
npm install
npm run dev                        # http://localhost:3000
```

> 참고: 이 저장소에는 `node_modules`와 lock 파일이 없습니다. `npm install`로 의존성을 설치하세요.
> 백엔드 CORS는 `http://localhost:3000` 을 허용하도록 설정돼 있습니다(포트 변경 시 백엔드 [WebConfig.java](../pumasi-egov/src/main/java/egovframework/pmsi/config/WebConfig.java)도 수정).

## End-to-End 데모 시나리오
1. 사용자 = **u-owner** 선택 → `/forms/new`에서 폼 생성(최대 응답 5) → 질문 추가(예: 단일선택/선형배율/단답) → **게시**(크레딧 예치, 헤더 배지의 escrow 증가 확인).
2. 사용자 = **u-alice**로 전환 → `/feed`에서 해당 설문 **응답하기** → 성실히 작성·제출 → `통과(+N 크레딧)` 결과, 배지 잔액 증가 확인.
3. 사용자 = **u-bob** → 같은 설문을 열고 **즉시 제출**(고속) → `거절` 결과(크레딧 미지급).
4. 사용자 = **u-owner** → 대시보드 → 해당 설문 **결과 보기** → pass 응답만 반영된 차트 확인. 다른 계정으로 결과 페이지에 접근하면 블러 게이트가 뜹니다.

## 백엔드 연동 엔드포인트
- `POST /pmsi/form`, `POST /pmsi/form/{id}/questions`, `POST /pmsi/form/{id}/publish`
- `GET /pmsi/form/{id}`, `GET /pmsi/form/{id}/questions`, `GET /pmsi/form?ownerId=`
- `GET /pmsi/feed` (신규)
- `POST /pmsi/form/{id}/responses`
- `GET /pmsi/form/{id}/results`
- `GET /pmsi/credit/{userId}`

## 범위 밖
소셜 로그인, 하이브리드 피드 랭킹, 타깃팅 필터, 결제 UI, 엑셀 다운로드, 조건부 분기, 실시간(폴링/웹소켓) 대시보드.
