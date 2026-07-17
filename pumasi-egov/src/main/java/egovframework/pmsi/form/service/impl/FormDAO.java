package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.form.service.SectionVO;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("formDAO")
public class FormDAO extends EgovAbstractMapper {

    private static final String NS = "formMapper.";

    public void insertForm(FormVO vo) {
        getSqlSession().insert(NS + "insertForm", vo);
    }

    public void updateForm(FormVO vo) {
        getSqlSession().update(NS + "updateForm", vo);
    }

    public void updateClosesAt(String formId, OffsetDateTime closesAt) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("closesAt", closesAt);
        getSqlSession().update(NS + "updateClosesAt", p);
    }

    public void insertSection(String sectionId, String formId, int orderIndex, String title) {
        Map<String, Object> p = new HashMap<>();
        p.put("sectionId", sectionId);
        p.put("formId", formId);
        p.put("orderIndex", orderIndex);
        p.put("title", title);
        getSqlSession().insert(NS + "insertSection", p);
    }

    public void updateSection(String formId, String sectionId, String title) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("sectionId", sectionId);
        p.put("title", title);
        getSqlSession().update(NS + "updateSection", p);
    }

    public void deleteSection(String formId, String sectionId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("sectionId", sectionId);
        getSqlSession().delete(NS + "deleteSection", p);
    }

    public List<SectionVO> selectSections(String formId) {
        return getSqlSession().selectList(NS + "selectSections", formId);
    }

    public SectionVO selectSection(String formId, String sectionId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("sectionId", sectionId);
        return getSqlSession().selectOne(NS + "selectSection", p);
    }

    public int countSections(String formId) {
        Integer c = getSqlSession().selectOne(NS + "countSections", formId);
        return c == null ? 0 : c;
    }

    public int countQuestionsInSection(String sectionId) {
        Integer c = getSqlSession().selectOne(NS + "countQuestionsInSection", sectionId);
        return c == null ? 0 : c;
    }

    public FormVO selectForm(String formId) {
        return getSqlSession().selectOne(NS + "selectForm", formId);
    }

    public FormVO selectFormByShareToken(String shareToken) {
        return getSqlSession().selectOne(NS + "selectFormByShareToken", shareToken);
    }

    public FormVO selectFormForUpdate(String formId) {
        return getSqlSession().selectOne(NS + "selectFormForUpdate", formId);
    }

    public int countPassResponses(String formId) {
        Integer cnt = getSqlSession().selectOne(NS + "countPassResponses", formId);
        return cnt == null ? 0 : cnt;
    }

    public List<FormVO> selectFormList(String ownerId) {
        return getSqlSession().selectList(NS + "selectFormList", ownerId);
    }

    public List<FormVO> selectActiveFeed(String viewerId, int limit, int offset,
                                         Integer maxMinutes, Long minReward, boolean reciprocalOnly) {
        Map<String, Object> p = new HashMap<>();
        p.put("viewerId", viewerId);
        p.put("limit", limit);
        p.put("offset", offset);
        p.put("maxMinutes", maxMinutes);
        p.put("minReward", minReward);
        p.put("reciprocalOnly", reciprocalOnly);
        return getSqlSession().selectList(NS + "selectActiveFeed", p);
    }

    public List<Map<String, Object>> selectUnlockOpportunities(String viewerId, int limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("viewerId", viewerId);
        p.put("limit", limit);
        return getSqlSession().selectList(NS + "selectUnlockOpportunities", p);
    }

    public int countUnlockOpportunities(String viewerId) {
        Integer n = getSqlSession().selectOne(NS + "countUnlockOpportunities", viewerId);
        return n != null ? n : 0;
    }

    public List<Map<String, Object>> selectMyResponseActivity(String userId, int limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("limit", limit);
        return getSqlSession().selectList(NS + "selectMyResponseActivity", p);
    }

    public Map<String, Object> selectActiveFormByOwner(String ownerId, String viewerId) {
        Map<String, Object> p = new HashMap<>();
        p.put("ownerId", ownerId);
        p.put("viewerId", viewerId);
        return getSqlSession().selectOne(NS + "selectActiveFormByOwner", p);
    }

    /** 가드레일: ACTIVE → PAUSED */
    public void pause(String formId) {
        getSqlSession().update(NS + "pause", formId);
    }

    /** 소유자 재개: PAUSED → ACTIVE */
    public void resume(String formId) {
        getSqlSession().update(NS + "resume", formId);
    }

    public String selectFirstSectionId(String formId) {
        return getSqlSession().selectOne(NS + "selectFirstSectionId", formId);
    }

    public List<String> selectQuestionTypes(String formId) {
        return getSqlSession().selectList(NS + "selectQuestionTypes", formId);
    }

    public QuestionVO selectQuestion(String formId, String questionId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("questionId", questionId);
        return getSqlSession().selectOne(NS + "selectQuestion", p);
    }

    public void insertQuestion(QuestionVO vo) {
        getSqlSession().insert(NS + "insertQuestion", vo);
    }

    public void updateQuestion(QuestionVO vo) {
        getSqlSession().update(NS + "updateQuestion", vo);
    }

    public void deleteQuestion(String formId, String questionId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("questionId", questionId);
        getSqlSession().delete(NS + "deleteQuestion", p);
    }

    public void deleteOptions(String questionId) {
        getSqlSession().delete(NS + "deleteOptions", questionId);
    }

    public void updateQuestionOrder(String formId, String questionId, int orderIndex) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("questionId", questionId);
        p.put("orderIndex", orderIndex);
        getSqlSession().update(NS + "updateQuestionOrder", p);
    }

    public void insertOption(String optionId, String questionId, String label, int orderIndex) {
        Map<String, Object> p = new HashMap<>();
        p.put("optionId", optionId);
        p.put("questionId", questionId);
        p.put("label", label);
        p.put("orderIndex", orderIndex);
        getSqlSession().insert(NS + "insertOption", p);
    }

    public void publish(String formId, int cost, String shareToken) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("cost", cost);
        p.put("shareToken", shareToken);
        getSqlSession().update(NS + "publish", p);
    }

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
