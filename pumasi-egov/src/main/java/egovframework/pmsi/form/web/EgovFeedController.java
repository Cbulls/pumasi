package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 응답 피드 API.
 *
 *  GET /pmsi/feed   게시된 남의 설문 목록(본인 제외, 최신순)  (@CurrentUser = viewer)
 *
 * 하이브리드 랭킹(FeedRanker)은 범위 밖 — 스켈레톤은 최신순 단순 목록.
 */
@RestController
@RequestMapping("/pmsi/feed")
public class EgovFeedController {

    @Resource(name = "formService")
    private FormService formService;

    @GetMapping
    public List<FormVO> feed(@CurrentUser String userId) throws Exception {
        return formService.selectActiveFeed(userId);
    }
}
