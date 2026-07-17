package egovframework.pmsi.result.web;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.cmm.storage.StorageClient;
import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.result.service.impl.ExportJobDAO;
import egovframework.pmsi.result.service.impl.ExportJobWorker;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * 비동기 결과 내보내기 API (Phase 2 — 동기 CSV의 대용량 한계 대응).
 *
 *  POST /pmsi/form/{formId}/results/export-jobs               잡 생성(202)
 *  GET  /pmsi/form/{formId}/results/export-jobs/{jobId}        상태 조회
 *  GET  /pmsi/form/{formId}/results/export-jobs/{jobId}/download  완료 파일 다운로드
 */
@RestController
@RequestMapping("/pmsi/form/{formId}/results/export-jobs")
public class EgovExportJobController {

    @Resource(name = "formService")
    private FormService formService;

    @Resource(name = "exportJobDAO")
    private ExportJobDAO exportJobDAO;

    @Resource(name = "exportJobWorker")
    private ExportJobWorker exportJobWorker;

    @Resource(name = "storageClient")
    private StorageClient storage;

    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @PathVariable String formId,
            @CurrentUser String userId) throws Exception {
        requireOwner(formId, userId);
        String jobId = UUID.randomUUID().toString().replace("-", "");
        exportJobDAO.insertJob(jobId, formId, userId);
        exportJobWorker.run(jobId, formId, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("jobId", jobId));
    }

    @GetMapping("/{jobId}")
    public Map<String, Object> status(
            @PathVariable String formId,
            @PathVariable String jobId,
            @CurrentUser String userId) throws Exception {
        requireOwner(formId, userId);
        Map<String, Object> job = exportJobDAO.selectJob(formId, jobId);
        if (job == null) {
            throw PmsiException.notFound("export.notfound", "내보내기 잡 없음: " + jobId);
        }
        return job;
    }

    @GetMapping("/{jobId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable String formId,
            @PathVariable String jobId,
            @CurrentUser String userId) throws Exception {
        requireOwner(formId, userId);
        Map<String, Object> job = exportJobDAO.selectJob(formId, jobId);
        if (job == null) {
            throw PmsiException.notFound("export.notfound", "내보내기 잡 없음: " + jobId);
        }
        if (!"DONE".equals(job.get("status"))) {
            throw PmsiException.conflict("export.not.done", "아직 완료되지 않은 잡입니다: " + job.get("status"));
        }
        byte[] bytes = storage.get((String) job.get("storageKey"));
        if (bytes == null) {
            throw PmsiException.notFound("export.file.notfound", "내보내기 파일이 없습니다.");
        }
        String fileName = (String) job.getOrDefault("fileName", "export.csv");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"export.csv\"; filename*=UTF-8''" + encoded)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(bytes);
    }

    private void requireOwner(String formId, String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("export.forbidden", "본인 폼만 내보낼 수 있습니다.");
        }
    }
}
