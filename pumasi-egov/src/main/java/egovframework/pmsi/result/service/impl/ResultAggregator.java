package egovframework.pmsi.result.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 결과 집계기 — 순수 로직.
 *
 * pass만 집계. 분모 = 해당 질문에 답한 응답자 수.
 * 스케일: min~max 전 구간 0 채움. 주관식: 완전일치 빈도. FILE: 목록.
 */
public class ResultAggregator {

    static final int PIE_MAX_OPTIONS = 5;
    static final int TEXT_FREQ_TOP_N = 20;

    ChartData aggregate(QSpec q, List<RespData> responses) {
        List<RespData> valid = responses.stream()
                .filter(r -> "pass".equals(r.qualityFlag))
                .toList();
        return switch (q.type) {
            case "RADIO", "DROPDOWN", "CHECKBOX" -> aggregateChoice(q, valid);
            case "LINEAR_SCALE", "RATING" -> aggregateScale(q, valid);
            case "SHORT_TEXT", "LONG_TEXT", "DATE", "TIME" -> aggregateText(q, valid);
            case "FILE" -> aggregateFile(q, valid);
            case "MULTIPLE_CHOICE_GRID", "CHECKBOX_GRID" -> aggregateMatrix(q, valid);
            default -> {
                ChartData cd = new ChartData();
                cd.chartType = "unsupported";
                yield cd;
            }
        };
    }

    /** 그리드: counts 키 = "행=열". 분모 = 문항 응답자 수. */
    private ChartData aggregateMatrix(QSpec q, List<RespData> valid) {
        ChartData cd = new ChartData();
        cd.chartType = "matrix";
        cd.ratioSumMayExceed100 = "CHECKBOX_GRID".equals(q.type);
        List<String> rows = q.rowLabels != null ? q.rowLabels : List.of();
        List<String> cols = q.optionIds != null ? q.optionIds : List.of();
        for (String row : rows) {
            for (String col : cols) {
                cd.counts.put(row + "=" + col, 0);
            }
        }
        int respondents = 0;
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans == null || ans.isEmpty()) continue;
            boolean answered = false;
            for (String a : ans) {
                if (a != null && cd.counts.containsKey(a)) {
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

    private ChartData aggregateChoice(QSpec q, List<RespData> valid) {
        ChartData cd = new ChartData();
        boolean multi = "CHECKBOX".equals(q.type);
        cd.chartType = (!multi && q.optionIds.size() <= PIE_MAX_OPTIONS) ? "pie" : "bar";
        cd.ratioSumMayExceed100 = multi;

        for (String opt : q.optionIds) cd.counts.put(opt, 0);

        int respondents = 0;
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans == null || ans.isEmpty()) continue;
            boolean answered = false;
            for (String a : ans) {
                if (q.optionIds.contains(a)) {
                    cd.counts.merge(a, 1, Integer::sum);
                    answered = true;
                } else if (a != null && a.startsWith("기타:")) {
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
        int min = q.scaleMin != null ? q.scaleMin : 1;
        int max = q.scaleMax != null ? q.scaleMax : 5;
        if (min > max) {
            int t = min;
            min = max;
            max = t;
        }
        for (int i = min; i <= max; i++) {
            cd.counts.put(String.valueOf(i), 0);
        }

        List<Integer> values = new ArrayList<>();
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans == null || ans.isEmpty()) continue;
            try {
                int v = Integer.parseInt(ans.get(0).trim());
                if (v < min || v > max) continue;
                cd.counts.merge(String.valueOf(v), 1, Integer::sum);
                values.add(v);
            } catch (NumberFormatException ignored) { }
        }
        cd.respondentCount = values.size();
        if (!values.isEmpty()) {
            cd.average = values.stream().mapToInt(Integer::intValue).average().orElse(0);
            cd.median = median(values);
            for (var e : cd.counts.entrySet()) {
                cd.ratios.put(e.getKey(), 100.0 * e.getValue() / values.size());
            }
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
        cd.chartType = "text_freq";
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans != null && !ans.isEmpty() && !ans.get(0).isBlank()) {
                String t = ans.get(0).trim();
                cd.textResponses.add(t);
                freq.merge(t, 1, Integer::sum);
            }
        }
        cd.respondentCount = cd.textResponses.size();
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(freq.entrySet());
        ranked.sort(Comparator
                .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        int limit = Math.min(TEXT_FREQ_TOP_N, ranked.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> e = ranked.get(i);
            cd.counts.put(e.getKey(), e.getValue());
            if (cd.respondentCount > 0) {
                cd.ratios.put(e.getKey(), 100.0 * e.getValue() / cd.respondentCount);
            }
        }
        return cd;
    }

    private ChartData aggregateFile(QSpec q, List<RespData> valid) {
        ChartData cd = new ChartData();
        cd.chartType = "file_list";
        for (RespData r : valid) {
            List<String> ans = r.answers.get(q.id);
            if (ans != null && !ans.isEmpty() && !ans.get(0).isBlank()) {
                cd.textResponses.add(ans.get(0).trim());
            }
        }
        cd.respondentCount = cd.textResponses.size();
        return cd;
    }
}
