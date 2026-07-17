package egovframework.pmsi.result.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 비동기 내보내기 잡 DAO.
 */
@Repository("exportJobDAO")
public class ExportJobDAO extends EgovAbstractMapper {

    private static final String NS = "exportJobMapper.";

    public void insertJob(String jobId, String formId, String ownerId) {
        Map<String, Object> p = new HashMap<>();
        p.put("jobId", jobId);
        p.put("formId", formId);
        p.put("ownerId", ownerId);
        getSqlSession().insert(NS + "insertJob", p);
    }

    public void markRunning(String jobId) {
        getSqlSession().update(NS + "markRunning", jobId);
    }

    public void markDone(String jobId, String storageKey, String fileName) {
        Map<String, Object> p = new HashMap<>();
        p.put("jobId", jobId);
        p.put("storageKey", storageKey);
        p.put("fileName", fileName);
        getSqlSession().update(NS + "markDone", p);
    }

    public void markFailed(String jobId, String error) {
        Map<String, Object> p = new HashMap<>();
        p.put("jobId", jobId);
        p.put("error", error);
        getSqlSession().update(NS + "markFailed", p);
    }

    public Map<String, Object> selectJob(String formId, String jobId) {
        Map<String, Object> p = new HashMap<>();
        p.put("formId", formId);
        p.put("jobId", jobId);
        return getSqlSession().selectOne(NS + "selectJob", p);
    }
}
