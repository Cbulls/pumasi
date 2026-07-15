import java.util.*;

/** 질문 스펙 (집계 기준) */
class QSpec {
    String id, type;
    List<String> optionIds = List.of();
    Integer scaleMin, scaleMax;
    static QSpec choice(String id, String type, List<String> opts) {
        QSpec q = new QSpec(); q.id = id; q.type = type; q.optionIds = opts; return q;
    }
    static QSpec scale(String id, int min, int max) {
        QSpec q = new QSpec(); q.id = id; q.type = "LINEAR_SCALE"; q.scaleMin = min; q.scaleMax = max; return q;
    }
    static QSpec text(String id, String type) {
        QSpec q = new QSpec(); q.id = id; q.type = type; return q;
    }
}

/** 응답 1건 (집계 입력) */
class RespData {
    String qualityFlag;                       // pass / hold / reject
    Map<String, List<String>> answers;
    RespData(String flag, Map<String, List<String>> answers) {
        this.qualityFlag = flag; this.answers = answers;
    }
}

/** 차트 데이터 (프론트 렌더용) */
class ChartData {
    String chartType;                         // pie / bar / histogram / text_list
    Map<String, Integer> counts = new LinkedHashMap<>();
    Map<String, Double> ratios = new LinkedHashMap<>();
    int respondentCount;
    double average;
    double median;
    List<String> textResponses = new ArrayList<>();
}

/**
 * 결과 집계기. 질문 유형별 차트 데이터 생성.
 *
 * 공통 원칙:
 *  - pass 응답만 집계(불성실 reject/hold 제외) — baseline과 같은 원리.
 *  - 분모 = 해당 질문에 답한 응답자 수(무응답 제외).
 *  - 보기 순서 보존, 선택 0인 보기도 0으로 표시.
 *  - 차트 타입 힌트: 단일선택 보기 ≤5 → pie, 그 외/체크박스 → bar.
 */
public class ResultAggregator {

    static final int PIE_MAX_OPTIONS = 5;

    ChartData aggregate(QSpec q, List<RespData> responses) {
        List<RespData> valid = responses.stream()
            .filter(r -> "pass".equals(r.qualityFlag))
            .toList();
        return switch (q.type) {
            case "RADIO", "DROPDOWN", "CHECKBOX" -> aggregateChoice(q, valid);
            case "LINEAR_SCALE", "RATING" -> aggregateScale(q, valid);
            case "SHORT_TEXT", "LONG_TEXT" -> aggregateText(q, valid);
            default -> { ChartData cd = new ChartData(); cd.chartType = "unsupported"; yield cd; }
        };
    }

    private ChartData aggregateChoice(QSpec q, List<RespData> valid) {
        ChartData cd = new ChartData();
        boolean multi = "CHECKBOX".equals(q.type);
        // 단일선택 + 보기 적으면 pie, 그 외 bar
        cd.chartType = (!multi && q.optionIds.size() <= PIE_MAX_OPTIONS) ? "pie" : "bar";

        for (String opt : q.optionIds) cd.counts.put(opt, 0);   // 순서 보존 + 0 표시

        int respondents = 0;
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans == null || ans.isEmpty()) continue;
            boolean answered = false;
            for (String a : ans) {
                if (q.optionIds.contains(a)) {
                    cd.counts.merge(a, 1, Integer::sum);
                    answered = true;
                }
            }
            if (answered) respondents++;
        }
        cd.respondentCount = respondents;
        if (respondents > 0) {
            for (var e : cd.counts.entrySet()) {
                cd.ratios.put(e.getKey(), 100.0 * e.getValue() / respondents);
            }
        }
        return cd;
    }

    private ChartData aggregateScale(QSpec q, List<RespData> valid) {
        ChartData cd = new ChartData();
        cd.chartType = "histogram";
        List<Integer> values = new ArrayList<>();
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans == null || ans.isEmpty()) continue;
            try {
                int v = Integer.parseInt(ans.get(0));
                cd.counts.merge(String.valueOf(v), 1, Integer::sum);
                values.add(v);
            } catch (NumberFormatException ignored) { }
        }
        cd.respondentCount = values.size();
        if (!values.isEmpty()) {
            cd.average = values.stream().mapToInt(Integer::intValue).average().orElse(0);
            cd.median = median(values);
        }
        return cd;
    }

    private double median(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private ChartData aggregateText(QSpec q, List<RespData> valid) {
        ChartData cd = new ChartData();
        cd.chartType = "text_list";
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans != null && !ans.isEmpty() && !ans.get(0).isBlank()) {
                cd.textResponses.add(ans.get(0));
            }
        }
        cd.respondentCount = cd.textResponses.size();
        return cd;
    }
}
