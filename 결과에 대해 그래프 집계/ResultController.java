package com.pumasiform.result;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 결과 조회·다운로드 API.
 *
 * GET /api/forms/{id}/results/charts          그래프 데이터(질문 유형별 집계, pass만)
 * GET /api/forms/{id}/results/export.csv       CSV 다운로드(전체 응답)
 * GET /api/forms/{id}/results/export.xlsx      엑셀 다운로드(전체 응답)
 *
 * D7(결과 열람 무료)에 따라 크레딧 게이트 없음. 소유권만 검증.
 * 집계는 pass만(그래프 왜곡 방지), 내보내기는 전체(quality_flag로 구분).
 */
@RestController
@RequestMapping("/api/forms/{formId}/results")
public class ResultController {

    private final ResultService service;

    public ResultController(ResultService service) { this.service = service; }

    /** 그래프 데이터: 질문별 차트 데이터 목록 */
    @GetMapping("/charts")
    public List<Map<String, Object>> charts(@PathVariable String formId,
                                            @RequestHeader("X-User-Id") String userId) {
        return service.chartData(formId, userId);
    }

    /** CSV 다운로드 */
    @GetMapping("/export.csv")
    public ResponseEntity<ByteArrayResource> exportCsv(
            @PathVariable String formId,
            @RequestParam(defaultValue = "false") boolean expand,
            @RequestHeader("X-User-Id") String userId) {
        String csv = service.exportCsv(formId, userId, expand);
        // BOM 추가(엑셀에서 한글 깨짐 방지)
        byte[] bytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return fileResponse(bytes, "results.csv", "text/csv");
    }

    /** 엑셀 다운로드 */
    @GetMapping("/export.xlsx")
    public ResponseEntity<ByteArrayResource> exportXlsx(
            @PathVariable String formId,
            @RequestParam(defaultValue = "false") boolean expand,
            @RequestHeader("X-User-Id") String userId) {
        byte[] bytes = service.exportXlsx(formId, userId, expand);
        return fileResponse(bytes, "results.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private ResponseEntity<ByteArrayResource> fileResponse(byte[] bytes, String filename,
                                                           String contentType) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(contentType))
            .contentLength(bytes.length)
            .body(new ByteArrayResource(bytes));
    }
}
