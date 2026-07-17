package egovframework.pmsi.form.service;

import java.util.List;

/**
 * 폼 빌더 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 */
public interface FormService {

    String createForm(FormVO formVO) throws Exception;

    /** DRAFT: 제목·설명·maxResponses·closesAt / ACTIVE: closesAt만 */
    FormVO updateForm(String formId, FormVO patch, String userId) throws Exception;

    void addQuestion(QuestionVO questionVO, String userId) throws Exception;

    void updateQuestion(String formId, String questionId, QuestionVO patch, String userId) throws Exception;

    void deleteQuestion(String formId, String questionId, String userId) throws Exception;

    void reorderQuestions(String formId, List<String> questionIds, String userId) throws Exception;

    SectionVO addSection(String formId, String title, String userId) throws Exception;

    SectionVO updateSection(String formId, String sectionId, String title, String userId) throws Exception;

    void deleteSection(String formId, String sectionId, String userId) throws Exception;

    List<SectionVO> selectSectionsWithQuestions(String formId) throws Exception;

    void publishForm(String formId, String userId) throws Exception;

    void closeForm(String formId, String userId) throws Exception;

    /** 마감일시 경과 시 CLOSED + 잔여 escrow 환불(시스템) */
    void closeIfExpired(String formId) throws Exception;

    FormVO selectForm(String formId) throws Exception;

    FormVO selectFormByShareToken(String shareToken) throws Exception;

    FormVO selectFormForUpdate(String formId) throws Exception;

    int countPassResponses(String formId) throws Exception;

    void closeIfFull(String formId) throws Exception;

    List<FormVO> selectFormList(String ownerId) throws Exception;

    List<QuestionVO> selectQuestions(String formId) throws Exception;

    /** 페이지네이션 피드(정원 미달 ACTIVE만, 1:1 부스트 정렬) */
    List<FormVO> selectActiveFeed(String viewerId, int page, int size) throws Exception;

    /** 가드레일 일시정지(PAUSED) 해제 — 소유자만 */
    void resumeForm(String formId, String userId) throws Exception;

    /** 가드레일: 시스템이 ACTIVE 폼을 자동 일시정지 */
    void pauseForGuardrail(String formId) throws Exception;
}
