package egovframework.pmsi.response.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 응답 DAO — EgovAbstractMapper 상속(MyBatis).
 *
 * 1인 1회: survey_response(form_id, respondent_id) UNIQUE 로 DB가 보장.
 */
@Repository("responseDAO")
public class ResponseDAO extends EgovAbstractMapper {

    private static final String NS = "responseMapper.";

    public void insertResponse(String responseId, String formId, String respondentId,
                               String qualityFlag, int elapsedSeconds,
                               String anonLabel, OffsetDateTime consentAt) {
        Map<String, Object> p = new HashMap<>();
        p.put("responseId", responseId);
        p.put("formId", formId);
        p.put("respondentId", respondentId);
        p.put("qualityFlag", qualityFlag);
        p.put("elapsedSeconds", elapsedSeconds);
        p.put("anonLabel", anonLabel);
        p.put("consentAt", consentAt);
        getSqlSession().insert(NS + "insertResponse", p);
    }

    public void insertAnswer(String responseId, String questionId, String value) {
        Map<String, Object> p = new HashMap<>();
        p.put("responseId", responseId);
        p.put("questionId", questionId);
        p.put("value", value);
        getSqlSession().insert(NS + "insertAnswer", p);
    }

    /** 응답 시작 기록(재시작하면 started_at 갱신) — 서버 소요시간 측정용 */
    public void upsertSession(String formId, String respondentId) {
        getSqlSession().insert(NS + "upsertSession", sessionKey(formId, respondentId));
    }

    /** 응답 시작 시각(없으면 null) */
    public OffsetDateTime selectSessionStartedAt(String formId, String respondentId) {
        return getSqlSession().selectOne(NS + "selectSessionStartedAt", sessionKey(formId, respondentId));
    }

    /** 제출 완료 후 세션 정리 */
    public void deleteSession(String formId, String respondentId) {
        getSqlSession().delete(NS + "deleteSession", sessionKey(formId, respondentId));
    }

    /** 해당 폼에 응답을 제출했는지 */
    public boolean existsByFormAndRespondent(String formId, String respondentId) {
        Integer cnt = getSqlSession().selectOne(NS + "existsByFormAndRespondent",
                sessionKey(formId, respondentId));
        return cnt != null && cnt > 0;
    }

    /** HOLD 검토용 응답 메타(responseId/respondentId/qualityFlag). 없으면 null */
    public Map<String, Object> selectResponseMeta(String formId, String responseId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("responseId", responseId);
        return getSqlSession().selectOne(NS + "selectResponseMeta", p);
    }

    public void updateQualityFlag(String formId, String responseId, String qualityFlag) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("responseId", responseId);
        p.put("qualityFlag", qualityFlag);
        getSqlSession().update(NS + "updateQualityFlag", p);
    }

    /** 가드레일: 최근 limit건의 품질 플래그(최신순) */
    public List<String> selectRecentQualityFlags(String formId, int limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("limit", limit);
        return getSqlSession().selectList(NS + "selectRecentQualityFlags", p);
    }

    private Map<String, Object> sessionKey(String formId, String respondentId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("respondentId", respondentId);
        return p;
    }
}
