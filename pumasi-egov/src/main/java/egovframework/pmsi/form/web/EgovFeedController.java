package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 응답 피드 API.
 *
 *  GET /pmsi/feed?page&size&maxMinutes&minReward&reciprocalOnly
 */
@RestController
@RequestMapping("/pmsi/feed")
public class EgovFeedController {

    @Resource(name = "formService")
    private FormService formService;

    @GetMapping
    public List<FormVO> feed(
            @CurrentUser String userId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "maxMinutes", required = false) Integer maxMinutes,
            @RequestParam(value = "minReward", required = false) Long minReward,
            @RequestParam(value = "reciprocalOnly", defaultValue = "false") boolean reciprocalOnly)
            throws Exception {
        return formService.selectActiveFeed(userId, page, size, maxMinutes, minReward, reciprocalOnly);
    }
}
