package egovframework.pmsi.result.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.result.service.CsvExport;
import egovframework.pmsi.result.service.ResultService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 결과 조회 API.
 *
 *  GET /pmsi/form/{formId}/results            차트(언락된 pass만)
 *  GET /pmsi/form/{formId}/results/responses  개별 표
 *  GET /pmsi/form/{formId}/results/export.csv 엑셀용 CSV
 */
@RestController
@RequestMapping("/pmsi/form/{formId}/results")
public class EgovResultController {

    @Resource(name = "resultService")
    private ResultService resultService;

    @GetMapping
    public Map<String, Object> results(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        return resultService.chartData(formId, userId);
    }

    /** 개별 응답 표(익명화, 소유자만) */
    @GetMapping("/responses")
    public Map<String, Object> responseTable(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        return resultService.responseTable(formId, userId);
    }

    /** 개별 응답 CSV 다운로드(소유자만, UTF-8 BOM, 한글 파일명) */
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        CsvExport export = resultService.exportCsv(formId, userId);
        byte[] bytes = export.body().getBytes(StandardCharsets.UTF_8);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String base = export.fileBaseName() + "_" + date;
        String asciiFallback = "pumasi-export_" + date + ".csv";
        String encoded = URLEncoder.encode(base + ".csv", StandardCharsets.UTF_8)
                .replace("+", "%20");
        String disposition = "attachment; filename=\"" + asciiFallback
                + "\"; filename*=UTF-8''" + encoded;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }
}
