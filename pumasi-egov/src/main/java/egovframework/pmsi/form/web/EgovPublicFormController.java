package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.form.service.SectionVO;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 공개 공유 링크 조회 — 인증 불필요.
 *
 *  GET /pmsi/public/forms/{shareToken}
 *  ACTIVE(+미마감) 폼의 메타·섹션·질문만 반환. 제출은 로그인 필요.
 */
@RestController
@RequestMapping("/pmsi/public/forms")
public class EgovPublicFormController {

    @Resource(name = "formService")
    private FormService formService;

    @GetMapping("/{shareToken}")
    public Map<String, Object> getByShareToken(@PathVariable String shareToken) throws Exception {
        FormVO form = formService.selectFormByShareToken(shareToken);
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.conflict("form.not.active", "게시 중인 설문이 아닙니다.");
        }
        if (form.getClosesAt() != null && !form.getClosesAt().isAfter(OffsetDateTime.now())) {
            throw PmsiException.conflict("form.closed", "응답 기한이 지난 설문입니다.");
        }

        List<SectionVO> sections = formService.selectSectionsWithQuestions(form.getFormId());
        List<QuestionVO> questions = formService.selectQuestions(form.getFormId());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("formId", form.getFormId());
        out.put("title", form.getTitle());
        out.put("description", form.getDescription());
        out.put("status", form.getStatus());
        out.put("costCredits", form.getCostCredits());
        out.put("maxResponses", form.getMaxResponses());
        out.put("closesAt", form.getClosesAt());
        // shareToken은 URL 경로에 이미 있음 — 응답 본문에 재노출하지 않음
        out.put("sections", sections);
        out.put("questions", questions);
        return out;
    }
}
