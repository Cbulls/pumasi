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

    /** 로그아웃: 세션 삭제 */
    public void deleteSession(String token) {
        getSqlSession().delete(NS + "deleteSession", token);
    }

    /** 해당 유저의 만료 세션 정리(로그인 시 호출) */
    public void deleteExpiredSessions(String userId, OffsetDateTime now) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("now", now);
        getSqlSession().delete(NS + "deleteExpiredSessions", p);
    }
}
