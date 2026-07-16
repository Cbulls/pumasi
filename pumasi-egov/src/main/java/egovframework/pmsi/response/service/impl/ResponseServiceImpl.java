package egovframework.pmsi.response.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.CreditService;
import egovframework.pmsi.credit.service.SettleCommand;
import egovframework.pmsi.credit.service.SettleResult;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.form.service.SectionVO;
import egovframework.pmsi.form.service.impl.FormValidator;
import egovframework.pmsi.response.service.AnswerVO;
import egovframework.pmsi.response.service.ResponseService;
import egovframework.pmsi.response.service.SubmitRequestVO;
import egovframework.pmsi.response.service.SubmitResultVO;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service("responseService")
public class ResponseServiceImpl extends EgovAbstractServiceImpl implements ResponseService {

    @Resource(name = "responseDAO")
    private ResponseDAO responseDAO;

    @Resource(name = "formService")
    private FormService formService;

    @Resource(name = "creditService")
    private CreditService creditService;

    private final QualityJudge qualityJudge = new QualityJudge();
    private final AnswerValidator answerValidator = new AnswerValidator();

    @Override
    @Transactional
    public void start(String formId, String respondentId) throws Exception {
        FormVO form = formService.selectForm(formId);
        ensureActiveAccepting(form);
        if (form.getOwnerId().equals(respondentId)) {
            throw PmsiException.forbidden("response.own.form", "본인 설문에는 응답할 수 없습니다.");
        }
        responseDAO.upsertSession(formId, respondentId);
    }

    @Override
    @Transactional
    public SubmitResultVO submit(String formId, String respondentId, SubmitRequestVO req)
            throws Exception {

        FormVO form = formService.selectFormForUpdate(formId);
        if ("CLOSED".equals(form.getStatus())) {
            throw PmsiException.conflict("form.closed", "마감된 설문입니다.");
        }
        // 마감일시 경과 → 자동 마감 후 거절
        if (form.getClosesAt() != null && !form.getClosesAt().isAfter(OffsetDateTime.now())) {
            formService.closeIfExpired(formId);
            throw PmsiException.conflict("form.closed", "응답 기한이 지나 마감된 설문입니다.");
        }
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.active", "게시된 설문이 아닙니다.");
        }
        if (form.getOwnerId().equals(respondentId)) {
            throw PmsiException.forbidden("response.own.form", "본인 설문에는 응답할 수 없습니다.");
        }
        if (formService.countPassResponses(formId) >= form.getMaxResponses()) {
            throw PmsiException.conflict("form.full", "응답 정원이 가득 찼습니다.");
        }
        if (!req.isConsentAgreed()) {
            throw PmsiException.badRequest("response.consent.required",
                    "개인정보 수집·이용에 동의해야 응답을 제출할 수 있습니다.");
        }

        OffsetDateTime startedAt = responseDAO.selectSessionStartedAt(formId, respondentId);
        if (startedAt == null) {
            throw PmsiException.badRequest("response.session.required",
                    "응답 시작 기록이 없습니다. 설문을 다시 열어 주세요.");
        }
        int elapsedSeconds = (int) Math.max(0,
                Math.min(Integer.MAX_VALUE, Duration.between(startedAt, OffsetDateTime.now()).getSeconds()));

        List<QuestionVO> questions = formService.selectQuestions(formId);
        Map<String, QuestionVO> byId = new HashMap<>();
        for (QuestionVO q : questions) byId.put(q.getQuestionId(), q);

        List<AnswerVO> answers = req.getAnswers() == null ? List.of() : req.getAnswers();
        Map<String, AnswerVO> answerByQ = new HashMap<>();
        for (AnswerVO a : answers) answerByQ.put(a.getQuestionId(), a);

        List<SectionVO> sections = formService.selectSectionsWithQuestions(formId);
        Set<String> visitedSections = resolveVisitedSections(sections, answerByQ);

        // 분기 경로상 섹션의 필수 문항만 검사(안내 블록 제외)
        for (QuestionVO q : questions) {
            if (FormValidator.CONTENT_TYPES.contains(q.getType())) continue;
            if (!visitedSections.contains(q.getSectionId())) continue;
            if (q.isRequired() && isEmpty(answerByQ.get(q.getQuestionId()))) {
                throw PmsiException.badRequest("response.required",
                        "필수 문항에 응답하지 않았습니다: " + q.getTitle());
            }
        }

        // 미방문 섹션 답변은 무시하고 검증·저장에서 제외
        List<AnswerVO> visitedAnswers = new ArrayList<>();
        for (AnswerVO a : answers) {
            QuestionVO q = byId.get(a.getQuestionId());
            if (q == null) continue;
            if (!visitedSections.contains(q.getSectionId())) continue;
            if (FormValidator.CONTENT_TYPES.contains(q.getType())) continue;
            visitedAnswers.add(a);
        }

        List<String> valueErrors = answerValidator.validate(questions, visitedAnswers);
        if (!valueErrors.isEmpty()) {
            throw PmsiException.badRequest("response.invalid.value",
                    String.join(" / ", valueErrors));
        }

        List<Integer> singleChoicePositions = new ArrayList<>();
        List<String> textValues = new ArrayList<>();
        for (AnswerVO a : visitedAnswers) {
            QuestionVO q = byId.get(a.getQuestionId());
            if (q == null || a.getValues() == null || a.getValues().isEmpty()) continue;
            String first = a.getValues().get(0);
            switch (q.getType()) {
                case "RADIO" -> {
                    int pos = q.getOptions() == null ? -1 : q.getOptions().indexOf(first);
                    if (pos >= 0) singleChoicePositions.add(pos);
                }
                case "SHORT_TEXT", "LONG_TEXT" -> textValues.add(first);
                default -> { }
            }
        }
        int judgeCount = (int) questions.stream()
                .filter(q -> !FormValidator.CONTENT_TYPES.contains(q.getType()))
                .filter(q -> visitedSections.contains(q.getSectionId()))
                .count();
        QualityJudge.Flag flag = qualityJudge.judge(
                elapsedSeconds, Math.max(1, judgeCount),
                singleChoicePositions, textValues, null);

        String responseId = UUID.randomUUID().toString();
        String anonLabel = "익명-" + responseId.substring(0, 6);
        try {
            responseDAO.insertResponse(responseId, formId, respondentId,
                    flag.value(), elapsedSeconds, anonLabel, OffsetDateTime.now());
        } catch (DuplicateKeyException dup) {
            throw PmsiException.conflict("response.duplicate", "이미 이 설문에 응답했습니다.");
        }
        for (AnswerVO a : visitedAnswers) {
            if (a.getValues() == null) continue;
            for (String v : a.getValues()) {
                responseDAO.insertAnswer(responseId, a.getQuestionId(), v);
            }
        }
        responseDAO.deleteSession(formId, respondentId);

        long reward = 0;
        if (flag == QualityJudge.Flag.PASS) {
            SettleResult r = creditService.settle(new SettleCommand(
                    form.getOwnerId(), respondentId, form.getCostCredits(), responseId));
            reward = r.reward();
            formService.closeIfFull(formId);
        }
        return new SubmitResultVO(responseId, anonLabel, flag.value(), reward);
    }

    private void ensureActiveAccepting(FormVO form) throws Exception {
        if ("CLOSED".equals(form.getStatus())) {
            throw PmsiException.conflict("form.closed", "마감된 설문입니다.");
        }
        if (form.getClosesAt() != null && !form.getClosesAt().isAfter(OffsetDateTime.now())) {
            formService.closeIfExpired(form.getFormId());
            throw PmsiException.conflict("form.closed", "응답 기한이 지나 마감된 설문입니다.");
        }
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.active", "게시된 설문이 아닙니다.");
        }
    }

    /**
     * RADIO 분기 규칙을 따라 방문 섹션 집합을 계산한다.
     * 규칙이 없으면 선형으로 다음 섹션으로 진행한다.
     */
    private Set<String> resolveVisitedSections(List<SectionVO> sections,
                                               Map<String, AnswerVO> answerByQ) {
        if (sections == null || sections.isEmpty()) return Set.of();
        List<SectionVO> ordered = sections.stream()
                .sorted(Comparator.comparingInt(SectionVO::getOrderIndex))
                .toList();
        Map<String, SectionVO> byId = new HashMap<>();
        for (SectionVO s : ordered) byId.put(s.getSectionId(), s);

        Set<String> visited = new HashSet<>();
        SectionVO current = ordered.get(0);
        int guard = 0;
        while (current != null && guard++ < ordered.size() + 2) {
            visited.add(current.getSectionId());
            String nextId = null;
            for (QuestionVO q : current.getQuestions()) {
                if (!"RADIO".equals(q.getType())) continue;
                if (q.getBranchRules() == null || q.getBranchRules().isEmpty()) continue;
                AnswerVO a = answerByQ.get(q.getQuestionId());
                String choice = (a != null && a.getValues() != null && !a.getValues().isEmpty())
                        ? a.getValues().get(0) : null;
                if (choice != null && q.getBranchRules().containsKey(choice)) {
                    nextId = q.getBranchRules().get(choice);
                } else if (q.getBranchRules().containsKey("_default")) {
                    nextId = q.getBranchRules().get("_default");
                }
                if (nextId != null) break;
            }
            if (nextId != null) {
                current = byId.get(nextId);
                continue;
            }
            // 선형 다음 섹션
            int idx = ordered.indexOf(current);
            current = (idx >= 0 && idx + 1 < ordered.size()) ? ordered.get(idx + 1) : null;
        }
        return visited;
    }

    private boolean isEmpty(AnswerVO a) {
        if (a == null || a.getValues() == null || a.getValues().isEmpty()) return true;
        return a.getValues().stream().allMatch(v -> v == null || v.isBlank());
    }
}
