package egovframework.pmsi.form.service;

import java.util.List;

/**
 * 폼 빌더 서비스 인터페이스.
 *
 * ★ 표준프레임워크 규약 1: 서비스는 인터페이스 + ServiceImpl로 분리한다.
 *    (현재 품앗이폼은 @Service 단일 클래스였으나, 표준은 인터페이스를 강제)
 *
 * 학습 포인트:
 *  - 인터페이스 분리의 명분은 "구현 교체 가능성"이지만, 실무에선 대부분 1:1이다.
 *    그럼에도 표준프레임워크는 일관성·AOP 프록시·테스트 목적으로 이를 규약화했다.
 *  - 메서드에 throws Exception을 붙이는 게 표준 관례(EgovAbstractServiceImpl의
 *    예외 처리 흐름과 맞물림). 현대 Spring Boot는 unchecked 예외를 선호하지만,
 *    표준프레임워크 코드를 읽으려면 이 관례를 알아야 한다.
 */
public interface FormService {

    /** 폼 생성 → 생성된 formId 반환 */
    String createForm(FormVO formVO) throws Exception;

    /** 폼 조회(중첩 포함) */
    FormVO selectForm(String formId) throws Exception;

    /** 내 폼 목록 */
    List<FormVO> selectFormList(String ownerId) throws Exception;

    /** 질문 추가(검증 포함) */
    void addQuestion(QuestionVO questionVO) throws Exception;

    /** 폼 게시(DRAFT → ACTIVE) */
    void publishForm(String formId, String userId) throws Exception;
}
