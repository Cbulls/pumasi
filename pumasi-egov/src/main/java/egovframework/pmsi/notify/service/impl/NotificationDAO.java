package egovframework.pmsi.notify.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("notificationDAO")
public class NotificationDAO extends EgovAbstractMapper {

    private static final String NS = "notificationMapper.";

    public void insert(String id, String userId, String type, String title, String body,
                       String linkUrl, String refId) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", id);
        p.put("userId", userId);
        p.put("type", type);
        p.put("title", title);
        p.put("body", body);
        p.put("linkUrl", linkUrl);
        p.put("refId", refId);
        getSqlSession().insert(NS + "insert", p);
    }

    public List<Map<String, Object>> selectByUser(String userId, boolean unreadOnly, int limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("unreadOnly", unreadOnly);
        p.put("limit", limit);
        return getSqlSession().selectList(NS + "selectByUser", p);
    }

    public int countUnread(String userId) {
        Integer n = getSqlSession().selectOne(NS + "countUnread", userId);
        return n != null ? n : 0;
    }

    public void markRead(String userId, String id) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("id", id);
        getSqlSession().update(NS + "markRead", p);
    }

    public void markAllRead(String userId) {
        getSqlSession().update(NS + "markAllRead", userId);
    }
}
