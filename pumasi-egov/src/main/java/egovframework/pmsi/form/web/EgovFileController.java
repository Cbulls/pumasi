package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.cmm.storage.StorageClient;
import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.response.service.impl.ResponseDAO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 응답 파일 업로드/다운로드 (StorageClient: local 디스크 / S3 호환).
 *
 *  POST /pmsi/form/{formId}/files   multipart file
 *  GET  /pmsi/form/{formId}/files/{fileId}
 */
@RestController
@RequestMapping("/pmsi/form/{formId}/files")
public class EgovFileController {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain");

    @javax.annotation.Resource(name = "storageClient")
    private StorageClient storage;

    @javax.annotation.Resource(name = "formService")
    private FormService formService;

    @javax.annotation.Resource(name = "responseDAO")
    private ResponseDAO responseDAO;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(
            @PathVariable String formId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        if (!"ACTIVE".equals(form.getStatus()) && !"DRAFT".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.active", "파일을 올릴 수 없는 상태입니다.");
        }
        // DRAFT는 소유자만(미리보기용), ACTIVE는 응답자(비소유자) 또는 소유자
        if ("DRAFT".equals(form.getStatus()) && !form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("form.forbidden", "본인 폼에만 파일을 올릴 수 있습니다.");
        }
        if (file == null || file.isEmpty()) {
            throw PmsiException.badRequest("file.empty", "파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw PmsiException.badRequest("file.too.large", "파일은 5MB 이하여야 합니다.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType();
        if (!ALLOWED.contains(contentType)) {
            throw PmsiException.badRequest("file.type", "허용되지 않는 파일 형식입니다: " + contentType);
        }
        String original = sanitize(file.getOriginalFilename());
        String fileId = UUID.randomUUID().toString().replace("-", "") + "_" + original;
        storage.put(formId + "/" + fileId, file.getBytes(), contentType);

        return Map.of(
                "fileId", fileId,
                "fileName", original,
                "url", "/pmsi/form/" + formId + "/files/" + fileId);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> download(
            @PathVariable String formId,
            @PathVariable String fileId,
            @CurrentUser String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        boolean owner = form.getOwnerId().equals(userId);
        boolean respondent = responseDAO.existsByFormAndRespondent(formId, userId);
        if (!owner && !respondent) {
            throw PmsiException.forbidden("file.forbidden",
                    "폼 소유자이거나 해당 폼에 응답한 사용자만 파일을 받을 수 있습니다.");
        }
        if (fileId.contains("..") || fileId.contains("/") || fileId.contains("\\")) {
            throw PmsiException.badRequest("file.invalid", "잘못된 파일 ID입니다.");
        }
        byte[] bytes = storage.get(formId + "/" + fileId);
        if (bytes == null) {
            throw PmsiException.notFound("file.notfound", "파일 없음");
        }
        Resource res = new ByteArrayResource(bytes);
        String contentType = guessContentType(fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileId + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(res);
    }

    private String guessContentType(String fileId) {
        String lower = fileId.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        String base = Path.of(name).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._\\-가-힣]", "_");
    }
}
