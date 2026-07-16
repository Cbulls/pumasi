package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.CreditService;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.form.service.SectionVO;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service("formService")
public class FormServiceImpl extends EgovAbstractServiceImpl implements FormService {

    @Resource(name = "formDAO")
    private FormDAO formDAO;

    @Resource(name = "creditService")
    private CreditService creditService;

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
        formDAO.insertSection(newId(), formVO.getFormId(), 0, "섹션 1");
        return formVO.getFormId();
    }

    @Override
    @Transactional
    public FormVO updateForm(String formId, FormVO patch, String userId) throws Exception {
        FormVO form = requireOwner(formId, userId);
        if ("CLOSED".equals(form.getStatus())) {
            throw PmsiException.conflict("form.closed", "마감된 폼은 수정할 수 없습니다.");
        }
        if ("ACTIVE".equals(form.getStatus())) {
            // 게시 중에는 마감일시만 변경 가능
            formDAO.updateClosesAt(formId, patch.getClosesAt());
            return formDAO.selectForm(formId);
        }
        if (patch.getTitle() != null && !patch.getTitle().isBlank()) {
            form.setTitle(patch.getTitle().trim());
        }
        if (patch.getDescription() != null) {
            form.setDescription(patch.getDescription());
        }
        if (patch.getMaxResponses() > 0) {
            form.setMaxResponses(patch.getMaxResponses());
        }
        form.setClosesAt(patch.getClosesAt());
        formDAO.updateForm(form);
        return formDAO.selectForm(formId);
    }

    @Override
    @Transactional
    public void addQuestion(QuestionVO vo, String userId) throws Exception {
        FormVO form = requireOwnerDraft(vo.getFormId(), userId);
        normalizeContentType(vo);
        List<String> errors = validator.validate(vo);
        if (!errors.isEmpty()) {
            throw PmsiException.badRequest("form.validation.fail", String.join(" / ", errors));
        }
        String sectionId = vo.getSectionId();
        if (sectionId == null || sectionId.isBlank()) {
            sectionId = formDAO.selectFirstSectionId(vo.getFormId());
            vo.setSectionId(sectionId);
        } else {
            requireSection(vo.getFormId(), sectionId);
        }
        validateBranch(vo);
        vo.setQuestionId(newId());
        vo.setOrderIndex(formDAO.selectQuestionTypes(vo.getFormId()).size());
        formDAO.insertQuestion(vo);
        insertOptions(vo);
    }

    @Override
    @Transactional
    public void updateQuestion(String formId, String questionId, QuestionVO patch, String userId)
            throws Exception {
        requireOwnerDraft(formId, userId);
        QuestionVO existing = formDAO.selectQuestion(formId, questionId);
        if (existing == null) {
            throw PmsiException.notFound("question.notfound", "질문 없음: " + questionId);
        }
        patch.setQuestionId(questionId);
        patch.setFormId(formId);
        if (patch.getSectionId() == null || patch.getSectionId().isBlank()) {
            patch.setSectionId(existing.getSectionId());
        } else {
            requireSection(formId, patch.getSectionId());
        }
        normalizeContentType(patch);
        List<String> errors = validator.validate(patch);
        if (!errors.isEmpty()) {
            throw PmsiException.badRequest("form.validation.fail", String.join(" / ", errors));
        }
        validateBranch(patch);
        formDAO.updateQuestion(patch);
        formDAO.deleteOptions(questionId);
        insertOptions(patch);
    }

    @Override
    @Transactional
    public void deleteQuestion(String formId, String questionId, String userId) throws Exception {
        requireOwnerDraft(formId, userId);
        QuestionVO existing = formDAO.selectQuestion(formId, questionId);
        if (existing == null) {
            throw PmsiException.notFound("question.notfound", "질문 없음: " + questionId);
        }
        formDAO.deleteOptions(questionId);
        formDAO.deleteQuestion(formId, questionId);
    }

    @Override
    @Transactional
    public void reorderQuestions(String formId, List<String> questionIds, String userId) throws Exception {
        requireOwnerDraft(formId, userId);
        List<QuestionVO> current = formDAO.selectQuestions(formId);
        Set<String> all = new HashSet<>();
        for (QuestionVO q : current) all.add(q.getQuestionId());
        if (questionIds == null || questionIds.size() != all.size() || !all.containsAll(questionIds)) {
            throw PmsiException.badRequest("form.reorder.invalid",
                    "질문 ID 목록이 현재 질문 집합과 일치해야 합니다.");
        }
        int i = 0;
        for (String qid : questionIds) {
            formDAO.updateQuestionOrder(formId, qid, i++);
        }
    }

    @Override
    @Transactional
    public SectionVO addSection(String formId, String title, String userId) throws Exception {
        requireOwnerDraft(formId, userId);
        String t = (title == null || title.isBlank()) ? "새 섹션" : title.trim();
        int order = formDAO.countSections(formId);
        String sectionId = newId();
        formDAO.insertSection(sectionId, formId, order, t);
        SectionVO vo = new SectionVO();
        vo.setSectionId(sectionId);
        vo.setFormId(formId);
        vo.setTitle(t);
        vo.setOrderIndex(order);
        return vo;
    }

    @Override
    @Transactional
    public SectionVO updateSection(String formId, String sectionId, String title, String userId)
            throws Exception {
        requireOwnerDraft(formId, userId);
        SectionVO sec = requireSection(formId, sectionId);
        if (title == null || title.isBlank()) {
            throw PmsiException.badRequest("section.title.required", "섹션 제목은 필수입니다.");
        }
        formDAO.updateSection(formId, sectionId, title.trim());
        sec.setTitle(title.trim());
        return sec;
    }

    @Override
    @Transactional
    public void deleteSection(String formId, String sectionId, String userId) throws Exception {
        requireOwnerDraft(formId, userId);
        requireSection(formId, sectionId);
        if (formDAO.countSections(formId) <= 1) {
            throw PmsiException.conflict("section.last", "마지막 섹션은 삭제할 수 없습니다.");
        }
        if (formDAO.countQuestionsInSection(sectionId) > 0) {
            throw PmsiException.conflict("section.not.empty",
                    "질문이 있는 섹션은 삭제할 수 없습니다. 질문을 먼저 이동·삭제하세요.");
        }
        formDAO.deleteSection(formId, sectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SectionVO> selectSectionsWithQuestions(String formId) throws Exception {
        formDAO.selectForm(formId); // notFound
        List<SectionVO> sections = formDAO.selectSections(formId);
        List<QuestionVO> questions = selectQuestions(formId);
        Map<String, SectionVO> byId = new HashMap<>();
        for (SectionVO s : sections) byId.put(s.getSectionId(), s);
        for (QuestionVO q : questions) {
            SectionVO s = byId.get(q.getSectionId());
            if (s != null) s.getQuestions().add(q);
        }
        return sections;
    }

    @Override
    @Transactional
    public void publishForm(String formId, String userId) throws Exception {
        FormVO form = requireOwner(formId, userId);
        if (!"DRAFT".equals(form.getStatus())) {
            throw PmsiException.conflict("form.already.published", "이미 게시된 폼입니다.");
        }
        List<String> types = formDAO.selectQuestionTypes(formId);
        long answerable = types.stream().filter(t -> !FormValidator.CONTENT_TYPES.contains(t)).count();
        if (answerable == 0) {
            throw PmsiException.badRequest("form.empty", "응답 가능한 질문이 없는 폼은 게시할 수 없습니다.");
        }
        int cost = computeCost(types);
        long escrow = (long) cost * form.getMaxResponses();
        creditService.depositEscrow(form.getOwnerId(), escrow, formId);
        String shareToken = UUID.randomUUID().toString().replace("-", "");
        formDAO.publish(formId, cost, shareToken);
    }

    @Override
    @Transactional
    public void closeForm(String formId, String userId) throws Exception {
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
        refundRemainingEscrow(form);
        formDAO.close(formId);
    }

    @Override
    @Transactional
    public void closeIfExpired(String formId) throws Exception {
        FormVO form = formDAO.selectFormForUpdate(formId);
        if (form == null || !"ACTIVE".equals(form.getStatus())) return;
        if (form.getClosesAt() == null) return;
        // closesAt 시각이 지났으면(동일 시각 포함) 자동 마감 + escrow 환불
        if (form.getClosesAt().isAfter(OffsetDateTime.now())) return;
        refundRemainingEscrow(form);
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
    @Transactional(readOnly = true)
    public FormVO selectFormByShareToken(String shareToken) throws Exception {
        FormVO form = formDAO.selectFormByShareToken(shareToken);
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "공유 링크가 유효하지 않습니다.");
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

    private void refundRemainingEscrow(FormVO form) throws Exception {
        int passCount = formDAO.countPassResponses(form.getFormId());
        long remaining = (long) form.getCostCredits()
                * Math.max(0, form.getMaxResponses() - passCount);
        creditService.refundEscrow(form.getOwnerId(), remaining, form.getFormId());
    }

    private void insertOptions(QuestionVO vo) {
        if (vo.getOptions() == null) return;
        int oi = 0;
        for (String label : vo.getOptions()) {
            formDAO.insertOption(newId(), vo.getQuestionId(), label, oi++);
        }
    }

    private void normalizeContentType(QuestionVO vo) {
        if (FormValidator.CONTENT_TYPES.contains(vo.getType())) {
            vo.setRequired(false);
        }
    }

    private void validateBranch(QuestionVO vo) {
        if (vo.getBranchRules() == null || vo.getBranchRules().isEmpty()) return;
        List<SectionVO> sections = formDAO.selectSections(vo.getFormId());
        Map<String, Integer> orderById = new HashMap<>();
        for (SectionVO s : sections) orderById.put(s.getSectionId(), s.getOrderIndex());
        SectionVO current = formDAO.selectSection(vo.getFormId(), vo.getSectionId());
        int curOrder = current == null ? 0 : current.getOrderIndex();
        List<String> errors = validator.validateBranchRules(vo, orderById, curOrder);
        if (!errors.isEmpty()) {
            throw PmsiException.badRequest("form.branch.invalid", String.join(" / ", errors));
        }
    }

    private FormVO requireOwner(String formId, String userId) {
        FormVO form = formDAO.selectForm(formId);
        if (form == null) {
            throw PmsiException.notFound("form.notfound", "폼 없음: " + formId);
        }
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("form.forbidden", "본인 폼만 수정할 수 있습니다.");
        }
        return form;
    }

    private FormVO requireOwnerDraft(String formId, String userId) {
        FormVO form = requireOwner(formId, userId);
        if (!"DRAFT".equals(form.getStatus())) {
            throw PmsiException.conflict("form.not.draft", "게시된 폼은 질문을 수정할 수 없습니다.");
        }
        return form;
    }

    private SectionVO requireSection(String formId, String sectionId) {
        SectionVO sec = formDAO.selectSection(formId, sectionId);
        if (sec == null) {
            throw PmsiException.notFound("section.notfound", "섹션 없음: " + sectionId);
        }
        return sec;
    }

    private int computeCost(List<String> types) {
        int minutes = 0;
        for (String t : types) {
            if (FormValidator.CONTENT_TYPES.contains(t)) continue;
            minutes += "LONG_TEXT".equals(t) ? 2 : 1;
        }
        return Math.max(1, minutes);
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}
