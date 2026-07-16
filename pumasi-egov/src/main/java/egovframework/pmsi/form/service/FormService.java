package egovframework.pmsi.form.service;

import java.util.List;

/**
 * 폼 빌더 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 */
public interface FormService {

    /** 폼 생성(기본 섹션 1개 자동 생성) → formId */
    String createForm(FormVO formVO) throws Exception;

    /** 질문 추가(유형별 검증 포함). 소유자만 가능. */
    void addQuestion(QuestionVO questionVO, String userId) throws Exception;

    /** 게시(DRAFT → ACTIVE). 비용 자동 산출 + escrow 예치(cost × maxResponses). */
    void publishForm(String formId, String userId) throws Exception;

    /** 마감(ACTIVE → CLOSED). 소유자만 가능. 미소진 escrow는 환불. */
    void closeForm(String formId, String userId) throws Exception;

    /** 폼 단건 조회(owner/status/cost 포함) */
    FormVO selectForm(String formId) throws Exception;

    /**
     * 폼 단건 조회 + 행 잠금(FOR UPDATE).
     * 응답 제출 시 maxResponses 상한 검사를 직렬화하기 위해 사용한다.
     */
    FormVO selectFormForUpdate(String formId) throws Exception;

    /** pass 판정 응답 수(escrow를 소진한 응답 수) */
    int countPassResponses(String formId) throws Exception;

    /**
     * pass 응답이 maxResponses에 도달했으면 CLOSED로 전환.
     * 응답 제출 트랜잭션에서 정산 직후 호출된다(자동 마감).
     */
    void closeIfFull(String formId) throws Exception;

    /** 내 폼 목록 */
    List<FormVO> selectFormList(String ownerId) throws Exception;

    /** 폼의 질문 목록(옵션 포함) — 응답 수집/결과 집계 모듈이 재사용 */
    List<QuestionVO> selectQuestions(String formId) throws Exception;

    /** 응답 피드: 게시(ACTIVE)된 남의 설문 목록(본인 것 제외, 최신순) */
    List<FormVO> selectActiveFeed(String viewerId) throws Exception;
}
