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
 *   유효성 검증 → 응답/답변 저장 → quality 판정 → pass면 크레딧 정산.
 *   정산이 실패(예치금 소진 등)하면 전체 롤백 → 응답도 저장되지 않는다(원자성).
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

    @Override
    @Transactional
    public SubmitResultVO submit(String formId, String respondentId, SubmitRequestVO req)
            throws Exception {

        FormVO form = formService.selectForm(formId);   // 없으면 notFound
        if (!"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.active", "게시된 설문이 아닙니다.");
        }
        if (form.getOwnerId().equals(respondentId)) {
            throw PmsiException.forbidden("response.own.form", "본인 설문에는 응답할 수 없습니다.");
        }

        List<QuestionVO> questions = formService.selectQuestions(formId);
        Map<String, QuestionVO> byId = new HashMap<>();
        for (QuestionVO q : questions) byId.put(q.getQuestionId(), q);

        List<AnswerVO> answers = req.getAnswers() == null ? List.of() : req.getAnswers();
        Map<String, AnswerVO> answerByQ = new HashMap<>();
        for (AnswerVO a : answers) answerByQ.put(a.getQuestionId(), a);

        // 유효성(받느냐): 필수 문항 응답 확인
        for (QuestionVO q : questions) {
            if (q.isRequired() && isEmpty(answerByQ.get(q.getQuestionId()))) {
                throw PmsiException.badRequest("response.required",
                        "필수 문항에 응답하지 않았습니다: " + q.getTitle());
            }
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
        QualityJudge.Flag flag = qualityJudge.judge(
                req.getElapsedSeconds(), questions.size(),
                singleChoicePositions, textValues, req.getAttentionPassed());

        // 저장 (1인 1회: UNIQUE 위반이면 이미 응답)
        String responseId = UUID.randomUUID().toString();
        try {
            responseDAO.insertResponse(responseId, formId, respondentId,
                    flag.value(), req.getElapsedSeconds());
        } catch (DuplicateKeyException dup) {
            throw PmsiException.conflict("response.duplicate", "이미 이 설문에 응답했습니다.");
        }
        for (AnswerVO a : answers) {
            if (!byId.containsKey(a.getQuestionId()) || a.getValues() == null) continue;
            for (String v : a.getValues()) {
                responseDAO.insertAnswer(responseId, a.getQuestionId(), v);
            }
        }

        // pass만 크레딧 정산(reject/hold는 데이터만 저장, 크레딧 미지급)
        long reward = 0;
        if (flag == QualityJudge.Flag.PASS) {
            SettleResult r = creditService.settle(new SettleCommand(
                    form.getOwnerId(), respondentId, form.getCostCredits(), responseId));
            reward = r.reward();
        }
        return new SubmitResultVO(responseId, flag.value(), reward);
    }

    private boolean isEmpty(AnswerVO a) {
        if (a == null || a.getValues() == null || a.getValues().isEmpty()) return true;
        return a.getValues().stream().allMatch(v -> v == null || v.isBlank());
    }
}
