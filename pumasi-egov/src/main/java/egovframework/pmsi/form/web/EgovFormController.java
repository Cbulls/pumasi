package egovframework.pmsi.form.web;

import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;

import egovframework.pmsi.cmm.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 폼 빌더 API.
 *
 *  POST /pmsi/form                     폼 생성            (@CurrentUser)
 *  POST /pmsi/form/{id}/questions      질문 추가          (@CurrentUser, 소유자만)
 *  POST /pmsi/form/{id}/publish        게시(escrow 예치)  (@CurrentUser)
 *  POST /pmsi/form/{id}/close          마감 + 잔여 escrow 환불 (@CurrentUser, 소유자만)
 *  GET  /pmsi/form/{id}                폼 조회
 *  GET  /pmsi/form/{id}/questions      질문 목록
 *  GET  /pmsi/form                     내 폼 목록(토큰 주체 기준)
 *
 * 인증: 로그인 토큰(Bearer)에서 해소한 사용자(@CurrentUser)를 사용. X-User-Id 신뢰 제거.
 * 표준 관례대로 throws Exception 을 컨트롤러까지 전파, 전역 핸들러가 변환.
 */
@RestController
@RequestMapping("/pmsi/form")
public class EgovFormController {

    @Resource(name = "formService")
    private FormService formService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createForm(
            @Valid @RequestBody FormVO formVO,
            @CurrentUser String userId) throws Exception {
        formVO.setOwnerId(userId);
        String formId = formService.createForm(formVO);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("formId", formId));
    }

    @PostMapping("/{formId}/questions")
    public ResponseEntity<Void> addQuestion(
            @PathVariable String formId,
            @Valid @RequestBody QuestionVO questionVO,
            @CurrentUser String userId) throws Exception {
        questionVO.setFormId(formId);
        formService.addQuestion(questionVO, userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{formId}/publish")
    public ResponseEntity<FormVO> publishForm(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        formService.publishForm(formId, userId);
        return ResponseEntity.ok(formService.selectForm(formId));
    }

    @PostMapping("/{formId}/close")
    public ResponseEntity<FormVO> closeForm(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        formService.closeForm(formId, userId);
        return ResponseEntity.ok(formService.selectForm(formId));
    }

    @GetMapping("/{formId}")
    public FormVO selectForm(@PathVariable String formId) throws Exception {
        return formService.selectForm(formId);
    }

    @GetMapping("/{formId}/questions")
    public List<QuestionVO> selectQuestions(@PathVariable String formId) throws Exception {
        return formService.selectQuestions(formId);
    }

    /** 내 폼 목록 — ownerId 파라미터 신뢰 제거, 토큰 주체 기준으로만 조회(IDOR 방지) */
    @GetMapping
    public List<FormVO> selectFormList(@CurrentUser String userId) throws Exception {
        return formService.selectFormList(userId);
    }
}
