package egovframework.pmsi.event.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 응답 퍼널 이벤트 DAO — view / start / submit 수집·집계.
 */
@Repository("eventDAO")
public class EventDAO extends EgovAbstractMapper {

    private static final String NS = "eventMapper.";

    public void insertEvent(String formId, String userId, String eventType) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("userId", userId);
        p.put("eventType", eventType);
        getSqlSession().insert(NS + "insertEvent", p);
    }

    /** { viewCount, startCount, submitCount } */
    public Map<String, Object> selectFunnel(String formId) {
        Map<String, Object> m = getSqlSession().selectOne(NS + "selectFunnel", formId);
        return m == null ? Map.of("viewCount", 0, "startCount", 0, "submitCount", 0) : m;
    }
}
