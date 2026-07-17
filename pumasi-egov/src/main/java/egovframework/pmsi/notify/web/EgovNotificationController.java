package egovframework.pmsi.notify.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.notify.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pmsi/notifications")
public class EgovNotificationController {

    @Resource(name = "notificationService")
    private NotificationService notificationService;

    @GetMapping
    public Map<String, Object> list(
            @CurrentUser String userId,
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(value = "limit", defaultValue = "30") int limit) throws Exception {
        List<Map<String, Object>> items = notificationService.list(userId, unreadOnly, limit);
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        out.put("unreadCount", notificationService.unreadCount(userId));
        return out;
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @CurrentUser String userId, @PathVariable("id") String id) throws Exception {
        notificationService.markRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@CurrentUser String userId) throws Exception {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
