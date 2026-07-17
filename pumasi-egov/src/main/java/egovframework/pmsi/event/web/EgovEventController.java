package egovframework.pmsi.event.web;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.event.service.impl.EventDAO;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

/**
 * 응답 퍼널 이벤트 API (Phase 2 측정 파이프라인 최소본).
 *
 *  POST /pmsi/events                       {formId, eventType: view|start|submit}
 *  GET  /pmsi/form/{formId}/events/funnel  소유자용 퍼널 집계(view/start/submit + 완료율)
 */
@RestController
public class EgovEventController {

    private static final Set<String> TYPES = Set.of("view", "start", "submit");

    @Resource(name = "eventDAO")
    private EventDAO eventDAO;

    @Resource(name = "formService")
    private FormService formService;

    @PostMapping("/pmsi/events")
    public ResponseEntity<Void> record(
            @CurrentUser String userId,
            @RequestBody Map<String, String> body) throws Exception {
        String formId = body.get("formId");
        String eventType = body.get("eventType");
        if (formId == null || formId.isBlank() || !TYPES.contains(eventType)) {
            throw PmsiException.badRequest("event.invalid",
                    "formId와 eventType(view|start|submit)이 필요합니다.");
        }
        formService.selectForm(formId);   // 존재 검증(없으면 404)
        eventDAO.insertEvent(formId, userId, eventType);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pmsi/form/{formId}/events/funnel")
    public Map<String, Object> funnel(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("event.forbidden", "본인 폼의 퍼널만 볼 수 있습니다.");
        }
        Map<String, Object> f = eventDAO.selectFunnel(formId);
        int view = ((Number) f.getOrDefault("viewCount", 0)).intValue();
        int submit = ((Number) f.getOrDefault("submitCount", 0)).intValue();
        double completionRate = view > 0 ? 100.0 * submit / view : 0.0;
        return Map.of(
                "viewCount", view,
                "startCount", ((Number) f.getOrDefault("startCount", 0)).intValue(),
                "submitCount", submit,
                "completionRate", Math.round(completionRate * 10) / 10.0);
    }
}
