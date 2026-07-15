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

    /** 유효(미만료) 세션의 userId, 없으면 null */
    public String selectValidUserId(String token, OffsetDateTime now) {
        Map<String, Object> p = new HashMap<>();
        p.put("token", token);
        p.put("now", now);
        return getSqlSession().selectOne(NS + "selectValidUserId", p);
    }
}
