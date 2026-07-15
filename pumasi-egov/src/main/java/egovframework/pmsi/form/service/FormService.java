package egovframework.pmsi.form.service;

import java.util.List;

/**
 * 폼 빌더 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 */
public interface FormService {

    /** 폼 생성(기본 섹션 1개 자동 생성) → formId */
    String createForm(FormVO formVO) throws Exception;

    /** 질문 추가(유형별 검증 포함) */
    void addQuestion(QuestionVO questionVO) throws Exception;

    /** 게시(DRAFT → ACTIVE). 비용 자동 산출 + escrow 예치(cost × maxResponses). */
    void publishForm(String formId, String userId) throws Exception;

    /** 폼 단건 조회(owner/status/cost 포함) */
    FormVO selectForm(String formId) throws Exception;

    /** 내 폼 목록 */
    List<FormVO> selectFormList(String ownerId) throws Exception;

    /** 폼의 질문 목록(옵션 포함) — 응답 수집/결과 집계 모듈이 재사용 */
    List<QuestionVO> selectQuestions(String formId) throws Exception;

    /** 응답 피드: 게시(ACTIVE)된 남의 설문 목록(본인 것 제외, 최신순) */
    List<FormVO> selectActiveFeed(String viewerId) throws Exception;
}
