# 품앗이폼 프론트엔드 (pumasi-web)

> 제품 목표·전체 기능·개발 중 문제와 해결의 **장문 개요**는 저장소 루트 [README.md](../README.md)를 보세요.  
> 이 문서는 **화면·환경 변수·데모 시나리오**에 집중합니다.

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
| `/` | SEO/AEO 마케팅 랜딩(SSR, FAQ·JSON-LD) |
| `/guide` | 품앗이 규칙·유의사항 안내 (첫 방문 팝업에서도 링크) |
| `/home` | 내 설문 대시보드(내 폼 목록, 상태 배지 — PAUSED 시 "게시 재개" 버튼) |
| `/forms/new` | 폼 빌더(생성 → 질문 추가 → 게시/예치). 응답형 11종(그리드·TIME 포함) + 주의 문항·기타/셔플. `?formId=`로 초안 이어 편집 |
| `/feed` | 응답 피드(게시된 남의 설문, 보상 미리보기, "더 보기" 페이지네이션) |
| `/forms/[id]/respond` | 응답(유형별 입력·**표형 그리드**, 진행률, 소요시간 측정 → 제출 결과). view/start/submit 퍼널 이벤트 기록 |
| `/forms/[id]/results` | 결과 대시보드(상호 언락된 pass 집계·**matrix 표**·퍼널·CSV, 응답 테이블에서 HOLD 승인/거절) |
| `/s/[token]` | 공개 공유 미리보기(동적 OG 메타) |

상단 헤더의 **계정 스위처**로 데모 계정(`u-owner`/`u-alice`/`u-bob`)을 전환합니다. 계정 선택 시 백엔드 `POST /pmsi/auth/login`으로 **로그인 토큰(Bearer)**을 발급받아 모든 요청에 `Authorization` 헤더로 첨부합니다(더 이상 `X-User-Id`를 신뢰하지 않음). 토큰은 `localStorage`에 보관됩니다. 크레딧 배지는 `/pmsi/credit/me`로 본인 잔액을 보여주고, **+충전** 버튼으로 Fake 크레딧을 구매할 수 있습니다(베타).

계정 스위처는 `NEXT_PUBLIC_DEMO_AUTH=true`일 때만 렌더됩니다. 프로덕션 빌드에서는 이 변수를 끄고 백엔드도 `PMSI_DEMO_AUTH=false`로 맞추세요.

## 사전 준비: 백엔드 실행
프론트는 백엔드가 떠 있어야 동작합니다.

```bash
cd ../pumasi-egov
docker compose up -d      # PostgreSQL
gradle bootRun            # http://localhost:8080
```

## 실행

```bash
cp .env.local.example .env.local   # API_BASE·SITE_URL 설정
npm install
npm run dev                        # http://localhost:3000
```

배포 시 `NEXT_PUBLIC_SITE_URL`을 실제 도메인(예: `https://pumasi.kr`)으로 맞추면 canonical·sitemap·OG가 올바르게 생성됩니다.

> 참고: `npm install`로 의존성을 설치하세요(`package-lock.json` 커밋됨 — CI는 `npm ci` 사용 가능).
> 백엔드 CORS는 기본 `http://localhost:3000` 을 허용합니다. 포트·도메인 변경 시 백엔드 환경 변수 `PMSI_CORS_ORIGINS`(콤마 구분)를 맞추세요.

## 그리드 문항 (프론트)

| 화면 | 동작 |
|------|------|
| 빌더 `QuestionEditor` | 행 목록 + 열(보기) 목록 편집. 유형: 객관식 그리드 / 체크박스 그리드. 체크 그리드는 행당 min/max |
| 응답 `AnswerInput` | 표 UI — MC는 행마다 radio, 체크는 칸 checkbox. `shuffleOptions`는 열만 섞음 |
| 결과 `ChartCard` | `chartType=matrix` → 행×열 카운트 표(+ 막대는 `"행=열"` 복합 키) |

답 값은 API에 `["맛=좋음","양=보통"]` 형태로 그대로 올라갑니다.

## End-to-End 데모 시나리오
1. 계정 = **u-owner** → `/forms/new`에서 폼 생성(최대 응답 5) → 질문 추가(예: 단일선택/드롭다운/별점/날짜/**객관식 그리드**) → **게시**(크레딧 예치, 배지의 escrow 증가 확인).
2. 계정 = **u-alice**로 전환 → `/feed`에서 해당 설문 **응답하기** → 개인정보 동의 체크 후 성실히 제출(그리드는 행마다 선택) → `통과(+N 크레딧)`, 익명 라벨(익명-xxxxxx) 표시, 배지 잔액 증가 확인.
3. 계정 = **u-bob** → 같은 설문을 열고 **즉시 제출**(고속) → `거절`(크레딧 미지급).
4. **상호 언락 확인**: 계정 = **u-owner** → 결과 페이지에서 u-alice의 응답이 **잠금** 상태(u-alice의 ACTIVE 설문에 아직 답하지 않았으므로). u-alice가 자기 설문을 게시해 두었다면, u-owner가 `/feed`에서 그 설문에 응답한 뒤 돌아오면 해당 행과 집계가 **열립니다**. 잠금 CTA는 상대 설문 딥링크를 안내합니다.
5. 결과 대시보드에서 언락된 pass 응답 차트(그리드는 **matrix 표**)·퍼널 지표(view→start→submit)·CSV 다운로드를 확인합니다. HOLD 응답이 있으면 응답 테이블에서 **승인**(소급 정산)/**거절**을 처리할 수 있습니다.

## 백엔드 연동 엔드포인트
- `POST /pmsi/auth/login` (토큰 발급)
- `POST /pmsi/form`, `POST /pmsi/form/{id}/questions`, `POST /pmsi/form/{id}/publish`, `POST /pmsi/form/{id}/resume`
- `GET /pmsi/form/{id}`, `GET /pmsi/form/{id}/questions`, `GET /pmsi/form`
- `GET /pmsi/feed?page&size` (무한 스크롤 페이지네이션)
- `POST /pmsi/form/{id}/responses` (동의 필수), `POST .../responses/{rid}/review` (HOLD 승인/거절)
- `GET /pmsi/form/{id}/results`, `.../results/responses`, `.../results/export.csv`
- `POST /pmsi/events`, `GET /pmsi/form/{id}/events/funnel`
- `GET /pmsi/credit/me`, `POST /pmsi/credit/purchase`

모든 경로(로그인 제외)는 `Authorization: Bearer <token>`가 필요합니다.

## 범위 밖
소셜 로그인 UI, 실PG 결제 UI(현재는 Fake 충전), 엑셀(xlsx) 다운로드(CSV·비동기 CSV는 지원), 퀴즈 채점 UI, 무로그인 응답, 타깃팅 필터, 실시간(폴링/웹소켓) 대시보드.
