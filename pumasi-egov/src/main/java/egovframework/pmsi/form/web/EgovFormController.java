package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.form.service.SectionVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

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

    @PutMapping("/{formId}")
    public FormVO updateForm(
            @PathVariable String formId,
            @RequestBody FormVO patch,
            @CurrentUser String userId) throws Exception {
        return formService.updateForm(formId, patch, userId);
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

    @PutMapping("/{formId}/questions/{questionId}")
    public ResponseEntity<Void> updateQuestion(
            @PathVariable String formId,
            @PathVariable String questionId,
            @Valid @RequestBody QuestionVO questionVO,
            @CurrentUser String userId) throws Exception {
        formService.updateQuestion(formId, questionId, questionVO, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{formId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable String formId,
            @PathVariable String questionId,
            @CurrentUser String userId) throws Exception {
        formService.deleteQuestion(formId, questionId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{formId}/questions/reorder")
    public ResponseEntity<Void> reorderQuestions(
            @PathVariable String formId,
            @RequestBody ReorderRequest body,
            @CurrentUser String userId) throws Exception {
        formService.reorderQuestions(formId, body.questionIds(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{formId}/sections")
    public List<SectionVO> selectSections(@PathVariable String formId) throws Exception {
        return formService.selectSectionsWithQuestions(formId);
    }

    @PostMapping("/{formId}/sections")
    public ResponseEntity<SectionVO> addSection(
            @PathVariable String formId,
            @RequestBody Map<String, String> body,
            @CurrentUser String userId) throws Exception {
        SectionVO sec = formService.addSection(formId, body.get("title"), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(sec);
    }

    @PutMapping("/{formId}/sections/{sectionId}")
    public SectionVO updateSection(
            @PathVariable String formId,
            @PathVariable String sectionId,
            @RequestBody Map<String, String> body,
            @CurrentUser String userId) throws Exception {
        return formService.updateSection(formId, sectionId, body.get("title"), userId);
    }

    @DeleteMapping("/{formId}/sections/{sectionId}")
    public ResponseEntity<Void> deleteSection(
            @PathVariable String formId,
            @PathVariable String sectionId,
            @CurrentUser String userId) throws Exception {
        formService.deleteSection(formId, sectionId, userId);
        return ResponseEntity.noContent().build();
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
    public FormVO selectForm(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        // shareToken은 소유자만 — 비소유자·피드 조회 시 노출 금지
        if (userId == null || !userId.equals(form.getOwnerId())) {
            form.setShareToken(null);
        }
        return form;
    }

    @GetMapping("/{formId}/questions")
    public List<QuestionVO> selectQuestions(@PathVariable String formId) throws Exception {
        return formService.selectQuestions(formId);
    }

    @GetMapping
    public List<FormVO> selectFormList(@CurrentUser String userId) throws Exception {
        return formService.selectFormList(userId);
    }

    public record ReorderRequest(@NotEmpty List<String> questionIds) {}
}
