package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.CreditService;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

/**
 * 폼 빌더 서비스 구현체.
 *
 * ★ 표준 규약: EgovAbstractServiceImpl 상속 + @Resource 이름 기반 주입.
 * ★ 검증은 순수 FormValidator 재사용(프레임워크와 무관하게 산다).
 * ★ escrow 예치는 CreditService 포트로 위임(모듈 간 의존을 인터페이스로 역전).
 *
 * ID 채번: 스켈레톤에서는 UUID를 쓴다. 표준 채번(EgovIdGnrService, ids 테이블)으로
 * 교체 가능하나, 무DB 의존으로 단순화하기 위해 UUID를 선택했다.
 */
@Service("formService")
public class FormServiceImpl extends EgovAbstractServiceImpl implements FormService {

    @Resource(name = "formDAO")
    private FormDAO formDAO;

    @Resource(name = "creditService")
    private CreditService creditService;

    /** 순수 검증 로직 재사용 */
    private final FormValidator validator = new FormValidator();

    @Override
    @Transactional
    public String createForm(FormVO formVO) throws Exception {
        if (formVO.getTitle() == null || formVO.getTitle().isBlank()) {
            throw PmsiException.badRequest("form.title.required", "폼 제목은 필수입니다.");
        }
        formVO.setFormId(newId());
        formVO.setStatus("DRAFT");
        if (formVO.getMaxResponses() <= 0) formVO.setMaxResponses(100);
        formDAO.insertForm(formVO);

        // 기본 섹션 1개 자동 생성
        formDAO.insertSection(newId(), formVO.getFormId(), 0, "섹션 1");
        return formVO.getFormId();
    }

    @Override
    @Transactional
    public void addQuestion(QuestionVO vo, String userId) throws Exception {
        FormVO form = formDAO.selectForm(vo.getFormId());
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "폼 없음: " + vo.getFormId());
        }
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("form.forbidden", "본인 폼에만 질문을 추가할 수 있습니다.");
        }
        if (!"DRAFT".equals(form.getStatus())) {
            throw PmsiException.conflict("form.not.draft", "게시된 폼에는 질문을 추가할 수 없습니다.");
        }

        // 검증: 순수 로직 그대로
        List<String> errors = validator.validate(vo);
        if (!errors.isEmpty()) {
            throw PmsiException.badRequest("form.validation.fail", String.join(" / ", errors));
        }

        // 섹션 미지정 시 첫 섹션에 붙임
        String sectionId = vo.getSectionId();
        if (sectionId == null || sectionId.isBlank()) {
            sectionId = formDAO.selectFirstSectionId(vo.getFormId());
            vo.setSectionId(sectionId);
        }

        vo.setQuestionId(newId());
        vo.setOrderIndex(formDAO.selectQuestionTypes(vo.getFormId()).size());   // 뒤에 추가
        formDAO.insertQuestion(vo);

        // 옵션 저장(cascade 없음 → 명시적으로)
        if (vo.getOptions() != null) {
            int oi = 0;
            for (String label : vo.getOptions()) {
                formDAO.insertOption(newId(), vo.getQuestionId(), label, oi++);
            }
        }
    }

    @Override
    @Transactional
    public void publishForm(String formId, String userId) throws Exception {
        FormVO form = formDAO.selectForm(formId);
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "폼 없음: " + formId);
        }
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("form.forbidden", "본인 폼만 게시할 수 있습니다.");
        }
        if (!"DRAFT".equals(form.getStatus())) {
            throw PmsiException.conflict("form.already.published", "이미 게시된 폼입니다.");
        }

        List<String> types = formDAO.selectQuestionTypes(formId);
        if (types.isEmpty()) {
            throw PmsiException.badRequest("form.empty", "질문이 없는 폼은 게시할 수 없습니다.");
        }

        int cost = computeCost(types);
        long escrow = (long) cost * form.getMaxResponses();

        // D7: 등록(게시) 시점에 cost × maxResponses 예치(escrow)
        creditService.depositEscrow(form.getOwnerId(), escrow, formId);

        formDAO.publish(formId, cost);
    }

    @Override
    @Transactional
    public void closeForm(String formId, String userId) throws Exception {
        // 행 잠금: 진행 중인 응답 제출(정산)과 직렬화해 escrow 계산이 어긋나지 않게 한다.
        FormVO form = formDAO.selectFormForUpdate(formId);
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "폼 없음: " + formId);
        }
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("form.forbidden", "본인 폼만 마감할 수 있습니다.");
        }
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.conflict("form.not.active", "게시 중인 폼만 마감할 수 있습니다.");
        }

        // 미소진 escrow 환불: pass 응답만 escrow(cost)를 소진했다.
        int passCount = formDAO.countPassResponses(formId);
        long remaining = (long) form.getCostCredits()
                * Math.max(0, form.getMaxResponses() - passCount);
        creditService.refundEscrow(form.getOwnerId(), remaining, formId);

        formDAO.close(formId);
    }

    @Override
    @Transactional(readOnly = true)
    public FormVO selectForm(String formId) throws Exception {
        FormVO form = formDAO.selectForm(formId);
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "폼 없음: " + formId);
        }
        return form;
    }

    @Override
    @Transactional
    public FormVO selectFormForUpdate(String formId) throws Exception {
        FormVO form = formDAO.selectFormForUpdate(formId);
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "폼 없음: " + formId);
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public int countPassResponses(String formId) throws Exception {
        return formDAO.countPassResponses(formId);
    }

    @Override
    @Transactional
    public void closeIfFull(String formId) throws Exception {
        FormVO form = formDAO.selectForm(formId);
        if (form == null || !"ACTIVE".equals(form.getStatus())) return;
        if (formDAO.countPassResponses(formId) >= form.getMaxResponses()) {
            // escrow는 cost × maxResponses 만큼 정확히 소진되었으므로 환불 없음
            formDAO.close(formId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormVO> selectFormList(String ownerId) throws Exception {
        return formDAO.selectFormList(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormVO> selectActiveFeed(String viewerId) throws Exception {
        return formDAO.selectActiveFeed(viewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionVO> selectQuestions(String formId) throws Exception {
        List<QuestionVO> questions = formDAO.selectQuestions(formId);
        for (QuestionVO q : questions) {
            if ("RADIO".equals(q.getType()) || "CHECKBOX".equals(q.getType())) {
                q.setOptions(formDAO.selectOptionLabels(q.getQuestionId()));
            }
        }
        return questions;
    }

    /**
     * 비용 자동 산출(D4): "소요시간 1분당 1크레딧" 근사.
     * 장문형은 2분, 그 외 유형은 1분으로 추정. 최소 1크레딧.
     */
    private int computeCost(List<String> types) {
        int minutes = 0;
        for (String t : types) {
            minutes += "LONG_TEXT".equals(t) ? 2 : 1;
        }
        return Math.max(1, minutes);
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}
