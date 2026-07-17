package egovframework.pmsi.result.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 결과 DAO — EgovAbstractMapper 상속(MyBatis).
 *
 * 집계·답변 공개는 상호응답으로 언락된 pass만. 응답자 ID는 API에 노출하지 않는다.
 */
@Repository("resultDAO")
public class ResultDAO extends EgovAbstractMapper {

    private static final String NS = "resultMapper.";

    /** pass + 언락된 응답의 답변 행 */
    public List<AnswerRow> selectPassAnswersUnlocked(String formId, String ownerId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("ownerId", ownerId);
        return getSqlSession().selectList(NS + "selectPassAnswersUnlocked", p);
    }

    public List<ResponseRow> selectResponses(String formId) {
        return getSqlSession().selectList(NS + "selectResponses", formId);
    }

    public List<AnswerRow> selectAllAnswers(String formId) {
        return getSqlSession().selectList(NS + "selectAllAnswers", formId);
    }

    /** owner가 설문에 응답해 둔 상대(폼 소유자) ID 집합 */
    public Set<String> selectUnlockedRespondentIds(String ownerId) {
        List<String> ids = getSqlSession().selectList(NS + "selectUnlockedRespondentIds", ownerId);
        return ids == null ? Set.of() : new HashSet<>(ids);
    }

    public UnlockTarget selectUnlockTarget(String ownerId, String respondentId) {
        Map<String, Object> p = new HashMap<>();
        p.put("ownerId", ownerId);
        p.put("respondentId", respondentId);
        return getSqlSession().selectOne(NS + "selectUnlockTarget", p);
    }
}
