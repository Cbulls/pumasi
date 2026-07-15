package egovframework.pmsi.response.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
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
}
