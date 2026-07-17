package egovframework.pmsi.result.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;
import egovframework.pmsi.form.service.impl.FormValidator;
import egovframework.pmsi.result.service.CsvExport;
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
import java.util.Set;

/**
 * 결과 조회 서비스.
 *
 * 품앗이 1:1 언락: A가 B 설문에 응답하면, B는 A의 설문에 응답해야 A의 답·집계를 본다.
 * 크레딧 예치(게시)와 병행. 응답자 ID는 API에 노출하지 않는다.
 */
@Service("resultService")
public class ResultServiceImpl extends EgovAbstractServiceImpl implements ResultService {

    private static final String LOCKED_PLACEHOLDER = "(잠김 — 상대 설문에 응답하면 열립니다)";

    @Resource(name = "resultDAO")
    private ResultDAO resultDAO;

    @Resource(name = "formService")
    private FormService formService;

    private final ResultAggregator aggregator = new ResultAggregator();

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> chartData(String formId, String userId) throws Exception {
        FormVO form = requireOwner(formId, userId);
        String ownerId = form.getOwnerId();

        ResponseStats stats = resultDAO.selectResponseStats(formId, ownerId);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalResponses", stats.getTotalResponses());
        summary.put("unlockedPassCount", stats.getUnlockedPassCount());
        summary.put("lockedCount", stats.getLockedCount());
        summary.put("passCount", stats.getPassCount());
        summary.put("holdCount", stats.getHoldCount());
        summary.put("rejectCount", stats.getRejectCount());
        summary.put("unlockedCount", stats.getUnlockedCount());
        double unlockRate = stats.getTotalResponses() == 0
                ? 0.0
                : (double) stats.getUnlockedCount() / stats.getTotalResponses();
        summary.put("unlockRate", unlockRate);

        List<QuestionVO> questions = formService.selectQuestions(formId);
        List<RespData> responses = loadUnlockedPassResponses(formId, ownerId);

        List<Map<String, Object>> items = new ArrayList<>();
        for (QuestionVO qvo : questions) {
            if (FormValidator.CONTENT_TYPES.contains(qvo.getType())) continue;
            QSpec q = toSpec(qvo);
            ChartData cd = aggregator.aggregate(q, responses);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", q.id);
            item.put("title", qvo.getTitle());
            item.put("type", qvo.getType());
            item.put("sectionId", qvo.getSectionId());
            item.put("chartType", cd.chartType);
            item.put("counts", cd.counts);
            item.put("ratios", cd.ratios);
            item.put("respondentCount", cd.respondentCount);
            item.put("average", cd.average);
            item.put("median", cd.median);
            item.put("textResponses", cd.textResponses);
            item.put("ratioSumMayExceed100", cd.ratioSumMayExceed100);
            items.add(item);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("summary", summary);
        out.put("items", items);
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> responseTable(String formId, String userId) throws Exception {
        FormVO form = requireOwner(formId, userId);
        String ownerId = form.getOwnerId();
        Set<String> unlockedIds = resultDAO.selectUnlockedRespondentIds(ownerId);

        List<QuestionVO> questions = formService.selectQuestions(formId);
        List<Map<String, Object>> qHeaders = new ArrayList<>();
        for (QuestionVO q : questions) {
            if (FormValidator.CONTENT_TYPES.contains(q.getType())) continue;
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("questionId", q.getQuestionId());
            h.put("title", q.getTitle());
            h.put("type", q.getType());
            qHeaders.add(h);
        }

        Map<String, Map<String, String>> answersByResponse = new HashMap<>();
        for (AnswerRow row : resultDAO.selectAllAnswers(formId)) {
            Map<String, String> cells =
                    answersByResponse.computeIfAbsent(row.getResponseId(), k -> new HashMap<>());
            cells.merge(row.getQuestionId(), row.getValue(),
                    (prev, next) -> prev + ", " + next);
        }

        int unlockedCount = 0;
        int lockedCount = 0;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ResponseRow r : resultDAO.selectResponses(formId)) {
            boolean unlocked = r.getRespondentId() != null
                    && unlockedIds.contains(r.getRespondentId());
            if (unlocked) unlockedCount++;
            else lockedCount++;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("responseId", r.getResponseId());   // HOLD 검토용(PII 아님)
            row.put("anonLabel", r.getAnonLabel());
            row.put("qualityFlag", r.getQualityFlag());
            row.put("submittedAt", r.getSubmittedAt());
            row.put("unlocked", unlocked);

            if (unlocked) {
                row.put("answers", answersByResponse.getOrDefault(r.getResponseId(), Map.of()));
            } else {
                Map<String, String> redacted = new HashMap<>();
                for (Map<String, Object> q : qHeaders) {
                    redacted.put(String.valueOf(q.get("questionId")), LOCKED_PLACEHOLDER);
                }
                row.put("answers", redacted);

                UnlockTarget target = resultDAO.selectUnlockTarget(ownerId, r.getRespondentId());
                if (target != null) {
                    row.put("unlockFormId", target.getFormId());
                    row.put("unlockFormTitle", target.getTitle());
                    row.put("unlockShareToken", target.getShareToken());
                    row.put("unlockHint", "이 응답자의 설문에 답하면 내용이 열립니다.");
                } else {
                    row.put("unlockHint", "상대가 게시 중인 설문이 없어 아직 열 수 없습니다.");
                }
            }
            rows.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("questions", qHeaders);
        out.put("rows", rows);
        out.put("unlockedCount", unlockedCount);
        out.put("lockedCount", lockedCount);
        out.put("reciprocityRule",
                "상대가 내 설문에 응답했으면, 그 사람의 설문에 응답해야 그 답을 볼 수 있습니다.");
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public CsvExport exportCsv(String formId, String userId) throws Exception {
        FormVO form = requireOwner(formId, userId);
        Map<String, Object> table = responseTable(formId, userId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> qHeaders = (List<Map<String, Object>>) table.get("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) table.get("rows");

        List<String> questionTitles = uniqueTitles(qHeaders);

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        List<String> headers = new ArrayList<>();
        headers.add("익명ID");
        headers.add("품질");
        headers.add("제출시각");
        headers.add("열림");
        headers.addAll(questionTitles);
        sb.append(headers.stream().map(this::csvEsc).reduce((a, b) -> a + "," + b).orElse(""));
        sb.append('\n');

        for (Map<String, Object> row : rows) {
            boolean unlocked = Boolean.TRUE.equals(row.get("unlocked"));
            @SuppressWarnings("unchecked")
            Map<String, String> answers = (Map<String, String>) row.getOrDefault("answers", Map.of());
            List<String> cells = new ArrayList<>();
            cells.add(String.valueOf(row.get("anonLabel")));
            cells.add(String.valueOf(row.get("qualityFlag")));
            cells.add(String.valueOf(row.get("submittedAt")));
            cells.add(unlocked ? "Y" : "N");
            for (Map<String, Object> q : qHeaders) {
                if (!unlocked) {
                    cells.add("");
                } else {
                    cells.add(answers.getOrDefault(String.valueOf(q.get("questionId")), ""));
                }
            }
            sb.append(cells.stream().map(this::csvEsc).reduce((a, b) -> a + "," + b).orElse(""));
            sb.append('\n');
        }
        return new CsvExport(sb.toString(), sanitizeFileBaseName(form.getTitle()));
    }

    /** 동일 제목이면 "제목", "제목 (2)", … */
    private List<String> uniqueTitles(List<Map<String, Object>> qHeaders) {
        Map<String, Integer> seen = new HashMap<>();
        List<String> out = new ArrayList<>();
        for (Map<String, Object> q : qHeaders) {
            String raw = String.valueOf(q.get("title"));
            if (raw == null || "null".equals(raw) || raw.isBlank()) raw = "문항";
            int n = seen.merge(raw, 1, Integer::sum);
            out.add(n == 1 ? raw : raw + " (" + n + ")");
        }
        return out;
    }

    static String sanitizeFileBaseName(String title) {
        String t = title == null || title.isBlank() ? "pumasi-export" : title.trim();
        t = t.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").replaceAll("\\s+", " ");
        if (t.length() > 60) t = t.substring(0, 60).trim();
        if (t.isBlank()) t = "pumasi-export";
        return t;
    }

    private FormVO requireOwner(String formId, String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("result.forbidden", "본인 설문의 결과만 볼 수 있습니다.");
        }
        return form;
    }

    private String csvEsc(String s) {
        String v = s == null ? "" : s;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private List<RespData> loadUnlockedPassResponses(String formId, String ownerId) {
        List<AnswerRow> rows = resultDAO.selectPassAnswersUnlocked(formId, ownerId);
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
