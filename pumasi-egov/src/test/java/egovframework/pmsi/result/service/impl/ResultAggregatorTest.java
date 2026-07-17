package egovframework.pmsi.result.service.impl;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResultAggregatorTest {

    private final ResultAggregator agg = new ResultAggregator();

    @Test
    void scaleFillsEmptyBinsBetweenMinAndMax() {
        QSpec q = new QSpec();
        q.id = "q1";
        q.type = "LINEAR_SCALE";
        q.scaleMin = 1;
        q.scaleMax = 5;

        RespData r = pass("q1", "5");
        ChartData cd = agg.aggregate(q, List.of(r));

        assertEquals("histogram", cd.chartType);
        assertEquals(5, cd.counts.size());
        assertEquals(0, cd.counts.get("1"));
        assertEquals(0, cd.counts.get("2"));
        assertEquals(0, cd.counts.get("3"));
        assertEquals(0, cd.counts.get("4"));
        assertEquals(1, cd.counts.get("5"));
        assertEquals(5.0, cd.average);
    }

    @Test
    void textBuildsExactMatchFrequency() {
        QSpec q = new QSpec();
        q.id = "q2";
        q.type = "SHORT_TEXT";

        ChartData cd = agg.aggregate(q, List.of(
                pass("q2", "좋아요"),
                pass("q2", "좋아요"),
                pass("q2", "보통")
        ));

        assertEquals("text_freq", cd.chartType);
        assertEquals(2, cd.counts.get("좋아요"));
        assertEquals(1, cd.counts.get("보통"));
        assertEquals(3, cd.respondentCount);
        assertEquals(3, cd.textResponses.size());
    }

    @Test
    void checkboxSetsRatioSumMayExceed100() {
        QSpec q = new QSpec();
        q.id = "q3";
        q.type = "CHECKBOX";
        q.optionIds = List.of("A", "B");

        Map<String, List<String>> answers = new HashMap<>();
        answers.put("q3", List.of("A", "B"));
        RespData r = new RespData("pass", answers);

        ChartData cd = agg.aggregate(q, List.of(r));
        assertTrue(cd.ratioSumMayExceed100);
        assertEquals(100.0, cd.ratios.get("A"));
        assertEquals(100.0, cd.ratios.get("B"));
    }

    @Test
    void fileListCollectsValues() {
        QSpec q = new QSpec();
        q.id = "q4";
        q.type = "FILE";

        ChartData cd = agg.aggregate(q, List.of(pass("q4", "/pmsi/form/x/files/a.png")));
        assertEquals("file_list", cd.chartType);
        assertEquals(1, cd.respondentCount);
        assertEquals("/pmsi/form/x/files/a.png", cd.textResponses.get(0));
    }

    @Test
    void matrixCountsRowColKeys() {
        QSpec q = new QSpec();
        q.id = "qg";
        q.type = "MULTIPLE_CHOICE_GRID";
        q.rowLabels = List.of("맛", "양");
        q.optionIds = List.of("좋음", "보통");

        Map<String, List<String>> a1 = new HashMap<>();
        a1.put("qg", List.of("맛=좋음", "양=보통"));
        Map<String, List<String>> a2 = new HashMap<>();
        a2.put("qg", List.of("맛=좋음", "양=좋음"));

        ChartData cd = agg.aggregate(q, List.of(
                new RespData("pass", a1),
                new RespData("pass", a2)));

        assertEquals("matrix", cd.chartType);
        assertEquals(2, cd.counts.get("맛=좋음"));
        assertEquals(1, cd.counts.get("양=보통"));
        assertEquals(1, cd.counts.get("양=좋음"));
        assertEquals(0, cd.counts.get("맛=보통"));
        assertEquals(2, cd.respondentCount);
        assertFalse(cd.ratioSumMayExceed100);
    }

    private static RespData pass(String qid, String value) {
        Map<String, List<String>> answers = new HashMap<>();
        answers.put(qid, List.of(value));
        return new RespData("pass", answers);
    }
}
