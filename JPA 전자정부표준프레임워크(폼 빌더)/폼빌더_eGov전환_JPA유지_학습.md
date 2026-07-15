# 폼 빌더 → 전자정부표준프레임워크 전환 결과 (JPA 유지) v1.0

> 학습용. 품앗이폼 폼 빌더를 표준프레임워크 규약으로 전환한 첫 모듈.
> 영속성은 JPA 유지(엔티티·cascade 보존), 계층 구조만 표준화.

---

## 0. 먼저 바로잡은 오해: JPA는 표준프레임워크에서 쓸 수 있다

표준프레임워크센터 공식 답변 기준:
- **MyBatis를 쓸 경우에만** DAO가 EgovAbstractMapper를 상속해야 한다.
- **JPA/Hibernate/Spring Data JPA는 정해진 규칙이 없다.** 자유롭게 구성하면 된다.
- 오히려 표준프레임워크의 데이터 처리 레이어는 **JPA를 표준 ORM으로 제시**하고
  구현체로 Hibernate를 쓴다.

즉 "표준프레임워크 = MyBatis"는 기술 강제가 아니라 SI 현장의 역사적 관례다.
JPA를 유지해도 표준 준수에 문제없다.

---

## 1. 무엇이 살고 무엇이 바뀌었나

| 요소 | 현재(품앗이폼) | 전환 후(eGov) | 변화 |
|---|---|---|---|
| 엔티티 | Form/Section/Question/QuestionOption | 동일 | ✅ 그대로 |
| cascade·orphanRemoval | JPA 설정 | 동일 | ✅ 그대로 |
| 검증 로직 | FormValidator(순수) | 동일 | ✅ 그대로 (19 테스트 통과 재확인) |
| QuestionType enum | 능력 정의 enum | 동일 | ✅ 그대로 |
| 패키지 | com.pumasiform.form | egovframework.pmsi.form.{web,service,service.impl} | 🔄 규약 변경 |
| 서비스 | @Service 단일 클래스 | FormService(if) + FormServiceImpl | 🔄 인터페이스 분리 |
| 서비스 상속 | 없음(POJO) | extends EgovAbstractServiceImpl | 🔄 표준 상속 |
| 리포지토리 | JpaRepository | JpaRepository(위치만 service.impl) | 🔄 위치만 |
| DI | 생성자 주입 | @Resource(name=...) | 🔄 관례 변경 |
| ID 채번 | UUID | EgovIdGnrService | 🔄 공통 컴포넌트 |
| 예외 | 커스텀 RuntimeException | processException()(표준) | 🔄 표준 예외 |
| DTO | record(불변) | VO(가변 JavaBean) | 🔄 관례 변경 |

핵심: **로직(엔티티·검증·enum)은 100% 보존**, 계층·관례만 표준화.

---

## 2. 검증: 로직 보존의 증명

표준 패키지(egovframework.pmsi.form.service.impl)로 옮긴 FormValidator가
원본 폼 빌더 테스트(FormValidationTest) 19개를 **그대로 통과**.

→ 패키지를 바꾸고 EgovAbstractServiceImpl 상속 구조로 감쌌어도, 비즈니스 규칙은
한 줄도 안 바뀌었다는 실증. "프레임워크 분리 설계"의 이득이 전환에서 드러난다.

---

## 3. 표준프레임워크 3대 규약 (이 모듈에서 익힌 것)

### 규약 1: 서비스 인터페이스 + ServiceImpl 분리
```
FormService (interface)  ←  표준이 강제
   └ FormServiceImpl (구현)
```
명분은 구현 교체 가능성. 실무는 대부분 1:1이지만 표준은 일관성·AOP·테스트 목적으로 규약화.

### 규약 2: EgovAbstractServiceImpl 상속
```java
public class FormServiceImpl extends EgovAbstractServiceImpl implements FormService
```
이 상속이 "표준 준수"의 핵심 증표. 얻는 것: processException()(표준 예외),
leaveaTrace()(로깅), egovLogger.

### 규약 3: @Resource 이름 기반 주입
```java
@Resource(name = "formService")     // 현대 Spring Boot의 생성자 주입과 대비
private FormService formService;
```

### 보너스: 공통 컴포넌트 (채번)
UUID → EgovIdGnrService(테이블 기반 채번). 표준의 진짜 가치 = 공통 기능 재사용.

---

## 4. JPA 유지 vs MyBatis 전환 (트레이드오프)

| | JPA 유지(이번 선택) | MyBatis 전환 |
|---|---|---|
| 엔티티 | 그대로 | VO + 매퍼 XML로 재작성 |
| SQL | 자동 생성 | 직접 작성 |
| cascade | 자동 | DAO에서 수동 처리 |
| 코드량 | 적음 | 보일러플레이트 많음 |
| SI 현장 체감 | 낮음 | 높음(실전형) |
| 학습 초점 | 계층·공통컴포넌트 | + SQL 통제·MyBatis |

→ JPA 유지는 표준의 **계층 구조와 공통 컴포넌트**에 집중하게 해준다.
   SI 현장 적응이 목표라면 다음 모듈은 MyBatis로 해보는 것도 추천.

---

## 5. 검증 상태 (정직한 한계)
- 검증 로직 보존: 표준 패키지로 옮겨 원본 19 테스트 통과 재확인 ✅
- Spring/표준 실행환경 빌드: 이 컨테이너에서 불가(Maven Central·표준 저장소 차단) ⚠️
  → 로컬에서 표준 저장소(maven.egovframe.go.kr) 접근 시 빌드 가능
- EgovAbstractServiceImpl·EgovIdGnrService 실제 동작: 표준 실행환경 라이브러리 필요 ⬜
- 채번 테이블(ids), JPA 설정(persistence): 로컬 구성 필요 ⬜

---

## 6. 다음 학습 단계
1. 로컬에 eGovFrame 4.x 개발환경 설치 → 이 코드 빌드·실행.
2. 응답 수집 모듈 전환(QualityJudge 순수 로직 재사용 체감).
3. (선택) 한 모듈은 MyBatis로 전환해 JPA와 비교.
4. 공통 컴포넌트(페이징 PaginationInfo, 권한) 적용.

---

## 7. 산출물
| 파일 | 역할 |
|---|---|
| `web/EgovFormController.java` | 컨트롤러(web 패키지, @Resource) |
| `service/FormService.java` | 서비스 인터페이스(규약 1) |
| `service/FormVO.java` `SectionVO` `QuestionVO` | VO(가변 JavaBean) |
| `service/impl/FormServiceImpl.java` | EgovAbstractServiceImpl 상속(규약 2) |
| `service/impl/FormJpaRepository.java` | JPA 유지(위치만 표준) |
| `service/impl/Form.java` `Section.java` `QuestionType.java` | 엔티티(그대로 보존) |
| `service/impl/FormValidator.java` | 검증 로직(그대로 보존, 19 테스트 통과) |
| `config/EgovIdGnrConfig.java` | 채번 공통 컴포넌트 |
| `build.gradle` | 표준 실행환경 의존성 + JPA |
