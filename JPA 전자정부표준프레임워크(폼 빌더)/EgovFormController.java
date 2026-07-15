package egovframework.pmsi.form.web;

import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 폼 빌더 컨트롤러.
 *
 * ★ 표준프레임워크 규약 3: 컨트롤러는 web 패키지에, 서비스는 @Resource 이름 기반 주입.
 *    (현재 품앗이폼은 생성자 주입이었음 — 둘 다 동작하나 표준 코드 관례는 @Resource)
 *
 * 표준 전통 방식은 .do URL + JSP 뷰지만, 여기선 REST(@RestController + JSON)를 유지한다.
 * 품앗이폼이 SPA/모바일 클라이언트를 가정하므로 REST가 맞고, 표준프레임워크도 4.x에서
 * REST를 문제없이 지원한다. 즉 "표준 준수 = JSP 강제"가 아니다.
 *
 * 학습 포인트:
 *  - throws Exception을 컨트롤러까지 전파(표준 관례). 전역 예외 핸들러가 받아 응답으로 변환.
 *  - 엔티티가 아니라 VO로 입출력(엔티티를 외부에 노출하지 않는 원칙은 현대 Spring과 동일).
 */
@RestController
@RequestMapping("/pmsi/form")
public class EgovFormController {

    @Resource(name = "formService")
    private FormService formService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createForm(
            @RequestBody FormVO formVO,
            @RequestHeader("X-User-Id") String userId) throws Exception {
        formVO.setOwnerId(userId);
        String formId = formService.createForm(formVO);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("formId", formId));
    }

    @GetMapping("/{formId}")
    public FormVO selectForm(@PathVariable String formId) throws Exception {
        return formService.selectForm(formId);
    }

    @GetMapping
    public List<FormVO> selectFormList(@RequestParam String ownerId) throws Exception {
        return formService.selectFormList(ownerId);
    }

    @PostMapping("/{formId}/questions")
    public ResponseEntity<Void> addQuestion(
            @PathVariable String formId,
            @RequestBody QuestionVO questionVO) throws Exception {
        questionVO.setFormId(formId);
        formService.addQuestion(questionVO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{formId}/publish")
    public ResponseEntity<Void> publishForm(
            @PathVariable String formId,
            @RequestHeader("X-User-Id") String userId) throws Exception {
        formService.publishForm(formId, userId);
        return ResponseEntity.ok().build();
    }
}
