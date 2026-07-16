package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 폼 DAO — EgovAbstractMapper 상속(MyBatis).
 *
 * JPA의 cascade가 없으므로 섹션/질문/옵션 저장을 명시적으로 처리한다
 * (전환 설계서 §3.5 학습 포인트: ORM이 숨기던 것을 직접 다룬다).
 */
@Repository("formDAO")
public class FormDAO extends EgovAbstractMapper {

    private static final String NS = "formMapper.";

    public void insertForm(FormVO vo) {
        getSqlSession().insert(NS + "insertForm", vo);
    }

    public void insertSection(String sectionId, String formId, int orderIndex, String title) {
        Map<String, Object> p = new HashMap<>();
        p.put("sectionId", sectionId);
        p.put("formId", formId);
        p.put("orderIndex", orderIndex);
        p.put("title", title);
        getSqlSession().insert(NS + "insertSection", p);
    }

    public FormVO selectForm(String formId) {
        return getSqlSession().selectOne(NS + "selectForm", formId);
    }

    /** 행 잠금 조회(FOR UPDATE) — 응답 상한 검사/마감의 직렬화용 */
    public FormVO selectFormForUpdate(String formId) {
        return getSqlSession().selectOne(NS + "selectFormForUpdate", formId);
    }

    /** pass 판정 응답 수(escrow 소진 건수) */
    public int countPassResponses(String formId) {
        Integer cnt = getSqlSession().selectOne(NS + "countPassResponses", formId);
        return cnt == null ? 0 : cnt;
    }

    public List<FormVO> selectFormList(String ownerId) {
        return getSqlSession().selectList(NS + "selectFormList", ownerId);
    }

    public List<FormVO> selectActiveFeed(String viewerId) {
        return getSqlSession().selectList(NS + "selectActiveFeed", viewerId);
    }

    public String selectFirstSectionId(String formId) {
        return getSqlSession().selectOne(NS + "selectFirstSectionId", formId);
    }

    /** publish 시 비용 산출용: 질문 유형 목록 */
    public List<String> selectQuestionTypes(String formId) {
        return getSqlSession().selectList(NS + "selectQuestionTypes", formId);
    }

    public void insertQuestion(QuestionVO vo) {
        getSqlSession().insert(NS + "insertQuestion", vo);
    }

    public void insertOption(String optionId, String questionId, String label, int orderIndex) {
        Map<String, Object> p = new HashMap<>();
        p.put("optionId", optionId);
        p.put("questionId", questionId);
        p.put("label", label);
        p.put("orderIndex", orderIndex);
        getSqlSession().insert(NS + "insertOption", p);
    }

    /** DRAFT → ACTIVE + 비용 확정 */
    public void publish(String formId, int cost) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("cost", cost);
        getSqlSession().update(NS + "publish", p);
    }

    /** ACTIVE → CLOSED */
    public void close(String formId) {
        getSqlSession().update(NS + "close", formId);
    }

    public List<QuestionVO> selectQuestions(String formId) {
        return getSqlSession().selectList(NS + "selectQuestions", formId);
    }

    public List<String> selectOptionLabels(String questionId) {
        return getSqlSession().selectList(NS + "selectOptionLabels", questionId);
    }
}
