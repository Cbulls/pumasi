package egovframework.pmsi.result.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.result.service.ResultService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 결과 조회 API.
 *
 *  GET /pmsi/form/{formId}/results   질문별 차트 데이터(pass만 집계)  (인증 필요, 소유자만)
 *
 * D7: 결과 열람 무료. 소유권만 검증(비소유자는 서비스에서 403).
 */
@RestController
@RequestMapping("/pmsi/form/{formId}/results")
public class EgovResultController {

    @Resource(name = "resultService")
    private ResultService resultService;

    @GetMapping
    public List<Map<String, Object>> results(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        return resultService.chartData(formId, userId);
    }

    /** 개별 응답 표(익명화, 소유자만) */
    @GetMapping("/responses")
    public Map<String, Object> responseTable(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        return resultService.responseTable(formId, userId);
    }
}
