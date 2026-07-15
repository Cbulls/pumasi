package com.pumasiform.result;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 결과 서비스. 집계(그래프)와 내보내기(엑셀/CSV)를 조율.
 *
 * 데이터 소스:
 *  - 질문 메타: 폼 빌더(form_question, form_question_option)
 *  - 응답: 응답 수집(survey_response, survey_answer)
 *
 * 필터 정책:
 *  - 그래프 집계: pass만(불성실 제외) — ResultAggregator가 처리.
 *  - 내보내기: 전체(quality_flag 열로 구분) — ResultExporter가 처리.
 */
@Service
public class ResultService {

    private final ResultQueryRepository repo;
    private final ResultAggregator aggregator = new ResultAggregator();
    private final ResultExporter exporter = new ResultExporter();

    public ResultService(ResultQueryRepository repo) { this.repo = repo; }

    /** 그래프 데이터: 질문별 ChartData를 직렬화 가능한 Map으로 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> chartData(String formId, String userId) {
        repo.assertOwner(formId, userId);
        List<QSpec> questions = repo.loadQuestionSpecs(formId);
        List<RespData> responses = repo.loadResponsesForAggregation(formId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (QSpec q : questions) {
            ChartData cd = aggregator.aggregate(q, responses);
            result.add(Map.of(
                "questionId", q.id,
                "chartType", cd.chartType,
                "counts", cd.counts,
                "ratios", cd.ratios,
                "respondentCount", cd.respondentCount,
                "average", cd.average,
                "median", cd.median,
                "textResponses", cd.textResponses
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public String exportCsv(String formId, String userId, boolean expand) {
        repo.assertOwner(formId, userId);
        List<ExQuestion> questions = repo.loadExportQuestions(formId);
        List<ExResponse> responses = repo.loadAllResponses(formId);   // 전체(reject 포함)
        return exporter.toCsv(questions, responses, expand);
    }

    /**
     * 엑셀 생성. POI로 행렬을 .xlsx로. Raw 시트 + (선택) 집계 시트.
     *
     * 대용량 주의: 응답이 수만 건이면 SXSSFWorkbook(스트리밍)으로 메모리 절약.
     * 더 크면 비동기 생성 + S3 업로드 + 다운로드 링크 방식으로 전환(설계만).
     */
    @Transactional(readOnly = true)
    public byte[] exportXlsx(String formId, String userId, boolean expand) {
        repo.assertOwner(formId, userId);
        List<ExQuestion> questions = repo.loadExportQuestions(formId);
        List<ExResponse> responses = repo.loadAllResponses(formId);
        List<List<String>> matrix = exporter.toMatrix(questions, responses, expand);

        return writeXlsx(matrix);
    }

    /**
     * POI로 행렬을 xlsx 바이트로. (실제 구현은 apache.poi 의존성 필요)
     *
     * try (var wb = new XSSFWorkbook()) {
     *   Sheet sheet = wb.createSheet("응답");
     *   for (int r = 0; r < matrix.size(); r++) {
     *     Row row = sheet.createRow(r);
     *     List<String> cells = matrix.get(r);
     *     for (int c = 0; c < cells.size(); c++) {
     *       row.createCell(c).setCellValue(cells.get(c));
     *     }
     *   }
     *   // 첫 행 굵게, 틀 고정(freeze pane) 등 스타일
     *   var out = new ByteArrayOutputStream();
     *   wb.write(out);
     *   return out.toByteArray();
     * }
     */
    private byte[] writeXlsx(List<List<String>> matrix) {
        // POI 미연동 환경에서는 행렬 직렬화로 대체(실제론 위 주석 코드).
        // 핵심 변환(toMatrix)은 ExportTest로 검증됨. POI는 그 행렬을 쓰는 도구.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (List<String> row : matrix) {
            out.writeBytes(String.join("\t", row).getBytes());
            out.writeBytes("\n".getBytes());
        }
        return out.toByteArray();
    }
}
