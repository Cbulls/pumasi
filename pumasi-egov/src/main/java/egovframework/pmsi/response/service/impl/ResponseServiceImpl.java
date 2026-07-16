package egovframework.pmsi.response.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.CreditService;
import egovframework.pmsi.credit.service.SettleCommand;
import egovframework.pmsi.credit.service.SettleResult;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 응답 수집 서비스 구현체.
 *
 * ★ 표준 규약: EgovAbstractServiceImpl 상속 + @Resource 이름 기반 주입.
 * ★ 포트 & 어댑터: 폼 조회는 FormService, 크레딧 정산은 CreditService 포트만 안다.
 *
 * 흐름(하나의 트랜잭션):
 *   폼 행 잠금 → 상한(maxResponses) 검사 → 유효성 검증 → 응답/답변 저장
 *   → quality 판정(소요시간은 서버 측정) → pass면 크레딧 정산 → 상한 도달 시 자동 마감.
 *   정산이 실패하면 전체 롤백 → 응답도 저장되지 않는다(원자성).
 */
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
        FormVO form = formService.selectForm(formId);   // 없으면 notFound
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.active", "게시된 설문이 아닙니다.");
        }
        if (form.getOwnerId().equals(respondentId)) {
            throw PmsiException.forbidden("response.own.form", "본인 설문에는 응답할 수 없습니다.");
        }
        responseDAO.upsertSession(formId, respondentId);
    }

    @Override
    @Transactional
    public SubmitResultVO submit(String formId, String respondentId, SubmitRequestVO req)
            throws Exception {

        // 행 잠금: 동시 제출 간 maxResponses 상한 검사를 직렬화(초과 정산 방지)
        FormVO form = formService.selectFormForUpdate(formId);   // 없으면 notFound
        if ("CLOSED".equals(form.getStatus())) {
            throw PmsiException.conflict("form.closed", "마감된 설문입니다.");
        }
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.active", "게시된 설문이 아닙니다.");
        }
        if (form.getOwnerId().equals(respondentId)) {
            throw PmsiException.forbidden("response.own.form", "본인 설문에는 응답할 수 없습니다.");
        }
        // 상한: pass 응답이 maxResponses에 도달했으면 받지 않는다(escrow 초과 방지)
        if (formService.countPassResponses(formId) >= form.getMaxResponses()) {
            throw PmsiException.conflict("form.full", "응답 정원이 가득 찼습니다.");
        }
        // 개인정보 수집·이용 동의 필수(개인정보보호법 준수)
        if (!req.isConsentAgreed()) {
            throw PmsiException.badRequest("response.consent.required",
                    "개인정보 수집·이용에 동의해야 응답을 제출할 수 있습니다.");
        }

        // 소요시간 서버 측정: start()가 기록한 시작 시각 기준(클라이언트 값 신뢰 제거)
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

        // 유효성(받느냐) 1: 필수 문항 응답 확인
        for (QuestionVO q : questions) {
            if (q.isRequired() && isEmpty(answerByQ.get(q.getQuestionId()))) {
                throw PmsiException.badRequest("response.required",
                        "필수 문항에 응답하지 않았습니다: " + q.getTitle());
            }
        }
        // 유효성(받느냐) 2: 답변 값이 질문 정의(보기/글자수/범위 등)를 지키는지
        List<String> valueErrors = answerValidator.validate(questions, answers);
        if (!valueErrors.isEmpty()) {
            throw PmsiException.badRequest("response.invalid.value",
                    String.join(" / ", valueErrors));
        }

        // 어뷰징(보상하느냐): quality 판정 입력 구성
        List<Integer> singleChoicePositions = new ArrayList<>();
        List<String> textValues = new ArrayList<>();
        for (AnswerVO a : answers) {
            QuestionVO q = byId.get(a.getQuestionId());
            if (q == null || a.getValues() == null || a.getValues().isEmpty()) continue;
            String first = a.getValues().get(0);
            switch (q.getType()) {
                case "RADIO" -> {
                    int pos = q.getOptions() == null ? -1 : q.getOptions().indexOf(first);
                    if (pos >= 0) singleChoicePositions.add(pos);
                }
                case "SHORT_TEXT", "LONG_TEXT" -> textValues.add(first);
                default -> { /* CHECKBOX/LINEAR_SCALE: 판정 입력에서 제외 */ }
            }
        }
        // 주의문항 기능은 미구현 → attentionPassed는 항상 null(클라이언트 입력 제거)
        QualityJudge.Flag flag = qualityJudge.judge(
                elapsedSeconds, questions.size(),
                singleChoicePositions, textValues, null);

        // 저장 (1인 1회: UNIQUE 위반이면 이미 응답)
        String responseId = UUID.randomUUID().toString();
        // 익명 라벨: 결과/조회에는 실제 respondent_id 대신 이 값만 노출한다.
        String anonLabel = "익명-" + responseId.substring(0, 6);
        try {
            responseDAO.insertResponse(responseId, formId, respondentId,
                    flag.value(), elapsedSeconds, anonLabel, OffsetDateTime.now());
        } catch (DuplicateKeyException dup) {
            throw PmsiException.conflict("response.duplicate", "이미 이 설문에 응답했습니다.");
        }
        for (AnswerVO a : answers) {
            if (!byId.containsKey(a.getQuestionId()) || a.getValues() == null) continue;
            for (String v : a.getValues()) {
                responseDAO.insertAnswer(responseId, a.getQuestionId(), v);
            }
        }
        responseDAO.deleteSession(formId, respondentId);

        // pass만 크레딧 정산(reject/hold는 데이터만 저장, 크레딧 미지급)
        long reward = 0;
        if (flag == QualityJudge.Flag.PASS) {
            SettleResult r = creditService.settle(new SettleCommand(
                    form.getOwnerId(), respondentId, form.getCostCredits(), responseId));
            reward = r.reward();
            // 상한 도달 시 자동 마감(escrow는 정확히 소진되어 환불 없음)
            formService.closeIfFull(formId);
        }
        return new SubmitResultVO(responseId, anonLabel, flag.value(), reward);
    }

    private boolean isEmpty(AnswerVO a) {
        if (a == null || a.getValues() == null || a.getValues().isEmpty()) return true;
        return a.getValues().stream().allMatch(v -> v == null || v.isBlank());
    }
}
