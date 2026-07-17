package egovframework.pmsi.response.web;

import egovframework.pmsi.response.service.ResponseService;
import egovframework.pmsi.response.service.SubmitRequestVO;
import egovframework.pmsi.response.service.SubmitResultVO;

import egovframework.pmsi.cmm.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 응답 수집 API.
 *
 *  POST /pmsi/form/{formId}/responses/start  응답 시작(서버 소요시간 측정 시작)
 *  POST /pmsi/form/{formId}/responses        응답 제출 (@CurrentUser = 응답자)
 *
 * 반환: 201 + { responseId, qualityFlag, rewardCredited }
 */
@RestController
@RequestMapping("/pmsi/form/{formId}/responses")
public class EgovResponseController {

    @Resource(name = "responseService")
    private ResponseService responseService;

    @PostMapping("/start")
    public ResponseEntity<Void> start(
            @PathVariable String formId,
            @CurrentUser String respondentId) throws Exception {
        responseService.start(formId, respondentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<SubmitResultVO> submit(
            @PathVariable String formId,
            @CurrentUser String respondentId,
            @Valid @RequestBody SubmitRequestVO req) throws Exception {
        SubmitResultVO result = responseService.submit(formId, respondentId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * HOLD 응답 검토(소유자 큐).
     * body: { "decision": "pass" | "reject" } — pass면 소급 정산(멱등)
     */
    @PostMapping("/{responseId}/review")
    public ResponseEntity<Void> review(
            @PathVariable String formId,
            @PathVariable String responseId,
            @CurrentUser String userId,
            @RequestBody java.util.Map<String, String> body) throws Exception {
        responseService.review(formId, responseId, userId, body.get("decision"));
        return ResponseEntity.noContent().build();
    }
}
