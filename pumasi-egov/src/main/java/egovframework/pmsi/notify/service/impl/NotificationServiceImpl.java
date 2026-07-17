package egovframework.pmsi.notify.service.impl;

import egovframework.pmsi.notify.service.NotificationService;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("notificationService")
public class NotificationServiceImpl extends EgovAbstractServiceImpl implements NotificationService {

    public static final String NEW_RESPONSE = "NEW_RESPONSE";
    public static final String UNLOCK_AVAILABLE = "UNLOCK_AVAILABLE";
    public static final String FORM_PAUSED = "FORM_PAUSED";
    public static final String HOLD_REVIEW = "HOLD_REVIEW";

    @Resource(name = "notificationDAO")
    private NotificationDAO notificationDAO;

    @Override
    @Transactional
    public void notify(String userId, String type, String title, String body, String linkUrl, String refId)
            throws Exception {
        if (userId == null || userId.isBlank()) return;
        String id = UUID.randomUUID().toString().replace("-", "");
        notificationDAO.insert(id, userId, type, title, body, linkUrl, refId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String userId, boolean unreadOnly, int limit) throws Exception {
        int safe = Math.min(100, Math.max(1, limit));
        return notificationDAO.selectByUser(userId, unreadOnly, safe);
    }

    @Override
    @Transactional(readOnly = true)
    public int unreadCount(String userId) throws Exception {
        return notificationDAO.countUnread(userId);
    }

    @Override
    @Transactional
    public void markRead(String userId, String id) throws Exception {
        notificationDAO.markRead(userId, id);
    }

    @Override
    @Transactional
    public void markAllRead(String userId) throws Exception {
        notificationDAO.markAllRead(userId);
    }
}
