package egovframework.pmsi.result.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 차트 데이터(프론트 렌더용). */
class ChartData {
    String chartType; // pie / bar / histogram / text_freq / text_list / file_list / unsupported
    Map<String, Integer> counts = new LinkedHashMap<>();
    Map<String, Double> ratios = new LinkedHashMap<>();
    int respondentCount;
    double average;
    double median;
    List<String> textResponses = new ArrayList<>();
    boolean ratioSumMayExceed100;
}
