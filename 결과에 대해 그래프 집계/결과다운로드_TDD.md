# 품앗이폼 — 결과 다운로드 (TDD) v1.0

수집된 응답을 그래프 데이터로 집계하고 엑셀/CSV로 내보내기. Red-Green-Refactor로 개발.

---

## 0. 핵심 설계: 집계는 pass만, 내보내기는 전부

같은 응답 데이터지만 용도에 따라 필터가 다르다.

| | 그래프 집계 | 엑셀/CSV 내보내기 |
|---|---|---|
| 목적 | 결과가 어떤가(요약) | raw 데이터 제공 |
| 필터 | pass만(불성실 제외) | 전체(quality_flag 열로 구분) |
| 이유 | reject/hold 섞이면 결과 왜곡 | 원천 데이터는 버리지 않음 |

> baseline을 pass 기준으로 낸 것과 같은 원리. 불성실 응답을 결과 그래프에 넣으면
> "이 설문 결과"가 오염된다. 반면 내보내기는 분석가가 직접 거르도록 전부 준다.

D7(결과 열람 무료)에 따라 크레딧 게이트 없음. 소유권만 검증.

---

## 1. 그래프 집계 (ResultAggregationTest, 25 테스트)

질문 유형마다 집계 방식이 다르다(폼 빌더 유형 체계와 맞물림).

| ID | 규칙 |
|---|---|
| G1 | RADIO/DROPDOWN: 보기별 카운트, 분모=응답자 수 |
| G2 | ★CHECKBOX★ 분모=응답자 수(선택 총횟수 아님). 복수 선택이라 비율 합>100% 가능 |
| G3 | ★불성실 제외★ pass만 집계 |
| G4 | LINEAR_SCALE/RATING: 값별 분포 + 평균 |
| G5 | 주관식: 집계 불가 → 응답 목록 |
| G6 | 무응답은 분모에서 제외 |
| G7 | 위조/삭제된 옵션 무시 |
| G8 | 단일선택 보기 ≤5 → pie, 그 외/체크박스 → bar (차트 타입 힌트) |
| G9 | 선형배율 중앙값 |

> G2가 가장 미묘: 한 사람이 3개 고르면 그 보기들 비율 합이 100%를 넘는 게 정상.
> 분모를 "선택 총횟수"로 하면 비율이 왜곡된다. 반드시 응답자 수로.

## 2. 엑셀/CSV 내보내기 (ExportTest, 18 테스트)

응답을 행렬(1행=1응답)로 변환. POI/CSV는 이 행렬을 쓰는 도구일 뿐, 핵심은 변환 로직.

| ID | 규칙 |
|---|---|
| X1/X2 | 헤더=[제출시각, quality_flag, 질문들], 1응답=1행 |
| X3 | ★체크박스 합치기★ 쉼표로 합침("빨강, 초록") — 사람용 |
| X4 | ★체크박스 펼치기★ 보기별 0/1 열로 분해 — 분석용 |
| X5 | 무응답 칸 빈 문자열 |
| X6 | ★CSV 이스케이프★ 쉼표/따옴표/줄바꿈(RFC 4180) |
| X7 | 옵션 ID가 아니라 라벨로 출력 |
| X8 | reject/hold도 포함(quality_flag로 구분) |

> X3/X4 두 모드를 둔 이유: 체크박스는 한 칸에 여러 값. 사람이 읽으려면 쉼표 합치기,
> 통계 돌리려면 보기별 0/1 펼치기. 용도가 다르므로 둘 다 제공(쿼리 파라미터 expand).
> CSV는 BOM(\uFEFF) 추가로 엑셀에서 한글 깨짐 방지.

---

## 3. Spring 구현 (REFACTOR)

### API
```
GET /api/forms/{id}/results/charts        그래프 데이터(질문별 집계, pass만)
GET /api/forms/{id}/results/export.csv     CSV(전체, ?expand=true 체크박스 펼치기)
GET /api/forms/{id}/results/export.xlsx    엑셀(전체)
```

### 데이터 소스와 모듈 경계
- 질문 메타: 폼 빌더(form_question, form_question_option)
- 응답: 응답 수집(survey_response, survey_answer)
- `ResultQueryRepository` 인터페이스로 분리 — 결과 모듈이 폼·응답 모듈 내부 엔티티에
  직접 의존하지 않게(응답 수집의 포트 패턴과 동일).
- 집계용 응답 로드는 `WHERE quality_flag='PASS'`로 DB에서 거름(성능).

### POI 엑셀
- 행렬(toMatrix) → XSSFWorkbook 셀 쓰기. 첫 행 굵게 + 틀 고정.
- 대용량: 수만 건이면 SXSSFWorkbook(스트리밍)으로 메모리 절약.
  더 크면 비동기 생성 + S3 + 다운로드 링크(설계만).

---

## 4. 검증 상태 (정직한 한계)
- 집계·내보내기 변환 로직: 순수 Java로 실제 RED→GREEN, 43 테스트 통과 ✅
- 서비스 조율(집계 pass-only / 내보내기 전체): 페이크 리포지토리로 실제 검증 ✅
- Spring 컨트롤러/서비스/리포지토리 인터페이스: 코드 작성·로직 대조 ✅, 빌드 미실행 ⚠️
- POI 엑셀 생성: writeXlsx는 환경 제약(POI 미연동)으로 행렬 직렬화로 대체.
  실제 XSSFWorkbook 코드는 주석으로 제시, 로컬에서 의존성 추가 후 활성화 필요 ⚠️
- ResultQueryRepository 구현체(폼·응답 모듈 조인 조회): 미구현 ⬜
- 미세 보강 TODO:
  - 교차분석(질문 A별 질문 B 분포) — 확장
  - 주관식 워드클라우드 데이터
  - 집계 시트를 엑셀 두 번째 시트로 추가

---

## 5. 산출물
| 파일 | 내용 |
|---|---|
| `tdd/ResultAggregationTest.java` | 집계 25 테스트 |
| `tdd/ResultAggregator.java` | 유형별 집계 로직 |
| `tdd/ExportTest.java` | 내보내기 18 테스트 |
| `tdd/ResultExporter.java` | 행렬 변환 + CSV 이스케이프 |
| `spring/.../ResultController.java` | 그래프·다운로드 API |
| `spring/.../ResultService.java` | 집계·내보내기 조율 + POI |
| `spring/.../ResultQueryRepository.java` | 데이터 로드 추상화 |
| `spring/.../ResultServiceTest.java` | 서비스 조율 테스트 |
