package egovframework.pmsi.notify.service;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    void notify(String userId, String type, String title, String body, String linkUrl, String refId)
            throws Exception;

    List<Map<String, Object>> list(String userId, boolean unreadOnly, int limit) throws Exception;

    int unreadCount(String userId) throws Exception;

    void markRead(String userId, String id) throws Exception;

    void markAllRead(String userId) throws Exception;
}
