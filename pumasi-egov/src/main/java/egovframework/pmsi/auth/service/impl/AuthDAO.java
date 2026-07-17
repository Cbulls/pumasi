package egovframework.pmsi.auth.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 인증 DAO — EgovAbstractMapper 상속(MyBatis).
 */
@Repository("authDAO")
public class AuthDAO extends EgovAbstractMapper {

    private static final String NS = "authMapper.";

    public boolean existsUser(String userId) {
        Integer cnt = getSqlSession().selectOne(NS + "existsUser", userId);
        return cnt != null && cnt > 0;
    }

    public void insertSession(String token, String userId, OffsetDateTime expiresAt) {
        Map<String, Object> p = new HashMap<>();
        p.put("token", token);
        p.put("userId", userId);
        p.put("expiresAt", expiresAt);
        getSqlSession().insert(NS + "insertSession", p);
    }

    public String selectValidUserId(String token, OffsetDateTime now) {
        Map<String, Object> p = new HashMap<>();
        p.put("token", token);
        p.put("now", now);
        return getSqlSession().selectOne(NS + "selectValidUserId", p);
    }

    public void deleteSession(String token) {
        getSqlSession().delete(NS + "deleteSession", token);
    }

    public void deleteExpiredSessions(String userId, OffsetDateTime now) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("now", now);
        getSqlSession().delete(NS + "deleteExpiredSessions", p);
    }

    public String selectUserIdByEmail(String email) {
        return getSqlSession().selectOne(NS + "selectUserIdByEmail", email);
    }

    public Map<String, Object> selectUserProfile(String userId) {
        return getSqlSession().selectOne(NS + "selectUserProfile", userId);
    }

    public void insertUser(String userId, String displayName, String email) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("displayName", displayName);
        p.put("email", email);
        getSqlSession().insert(NS + "insertUser", p);
    }

    public void insertMagicLink(String token, String email, String userId, OffsetDateTime expiresAt) {
        Map<String, Object> p = new HashMap<>();
        p.put("token", token);
        p.put("email", email);
        p.put("userId", userId);
        p.put("expiresAt", expiresAt);
        getSqlSession().insert(NS + "insertMagicLink", p);
    }

    public Map<String, Object> selectValidMagicLink(String token, OffsetDateTime now) {
        Map<String, Object> p = new HashMap<>();
        p.put("token", token);
        p.put("now", now);
        return getSqlSession().selectOne(NS + "selectValidMagicLink", p);
    }

    public void consumeMagicLink(String token) {
        getSqlSession().update(NS + "consumeMagicLink", token);
    }

    public void invalidateOpenMagicLinks(String email) {
        getSqlSession().update(NS + "invalidateOpenMagicLinks", email);
    }
}
