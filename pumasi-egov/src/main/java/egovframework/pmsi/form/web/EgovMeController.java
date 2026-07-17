package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 내 활동·언락 기회 API.
 */
@RestController
@RequestMapping("/pmsi/me")
public class EgovMeController {

    @Resource(name = "formService")
    private FormService formService;

    /** 상호 언락을 위해 응답할 상대 설문 */
    @GetMapping("/unlock-opportunities")
    public Map<String, Object> unlockOpportunities(
            @CurrentUser String userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) throws Exception {
        return formService.unlockOpportunities(userId, limit);
    }

    /** 내가 제출한 응답 이력 */
    @GetMapping("/responses")
    public Map<String, Object> myResponses(
            @CurrentUser String userId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) throws Exception {
        List<Map<String, Object>> items = formService.myResponseActivity(userId, limit);
        Map<String, Object> out = new HashMap<>();
        out.put("items", items);
        return out;
    }
}
