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
 *  GET /pmsi/feed?page=0&size=20   게시된 남의 설문 목록 (@CurrentUser = viewer)
 *  필터: 정원 미달 ACTIVE만.
 *  정렬: 1:1 부스트(내 설문에 응답해준 사람) → 채움률 낮은 순 → 최신순
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
            @RequestParam(value = "size", defaultValue = "20") int size) throws Exception {
        return formService.selectActiveFeed(userId, page, size);
    }
}
