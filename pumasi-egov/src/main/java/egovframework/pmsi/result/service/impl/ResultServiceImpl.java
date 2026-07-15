package egovframework.pmsi.result.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.result.service.ResultService;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 결과 조회 서비스 구현체.
 *
 * ★ 표준 규약: EgovAbstractServiceImpl 상속 + @Resource 이름 기반 주입.
 * ★ 집계는 순수 ResultAggregator 재사용. DAO는 pass 응답만 로드.
 */
@Service("resultService")
public class ResultServiceImpl extends EgovAbstractServiceImpl implements ResultService {

    @Resource(name = "resultDAO")
    private ResultDAO resultDAO;

    @Resource(name = "formService")
    private FormService formService;

    private final ResultAggregator aggregator = new ResultAggregator();

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> chartData(String formId, String userId) throws Exception {
        FormVO form = formService.selectForm(formId);        // 없으면 notFound
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("result.forbidden", "본인 설문의 결과만 볼 수 있습니다.");
        }

        List<QuestionVO> questions = formService.selectQuestions(formId);
        List<RespData> responses = loadPassResponses(formId);

        List<Map<String, Object>> out = new ArrayList<>();
        for (QuestionVO qvo : questions) {
            QSpec q = toSpec(qvo);
            ChartData cd = aggregator.aggregate(q, responses);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", q.id);
            item.put("title", qvo.getTitle());
            item.put("type", q.type);
            item.put("chartType", cd.chartType);
            item.put("counts", cd.counts);
            item.put("ratios", cd.ratios);
            item.put("respondentCount", cd.respondentCount);
            item.put("average", cd.average);
            item.put("median", cd.median);
            item.put("textResponses", cd.textResponses);
            out.add(item);
        }
        return out;
    }

    /** pass 답변 행을 응답 단위로 묶어 RespData 목록 구성 */
    private List<RespData> loadPassResponses(String formId) {
        List<AnswerRow> rows = resultDAO.selectPassAnswers(formId);
        Map<String, RespData> byResponse = new LinkedHashMap<>();
        for (AnswerRow row : rows) {
            RespData rd = byResponse.computeIfAbsent(row.getResponseId(),
                    k -> new RespData("pass", new HashMap<>()));
            rd.answers.computeIfAbsent(row.getQuestionId(), k -> new ArrayList<>())
                      .add(row.getValue());
        }
        return new ArrayList<>(byResponse.values());
    }

    private QSpec toSpec(QuestionVO qvo) {
        QSpec q = new QSpec();
        q.id = qvo.getQuestionId();
        q.type = qvo.getType();
        if (qvo.getOptions() != null) q.optionIds = qvo.getOptions();
        q.scaleMin = qvo.getScaleMin();
        q.scaleMax = qvo.getScaleMax();
        return q;
    }
}
