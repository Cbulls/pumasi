package egovframework.pmsi.form.web;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.impl.ImageAssetService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 문항 이미지 미디어 (리사이즈·WebP 파생본).
 *
 *  POST /pmsi/form/{formId}/media           소유자 업로드
 *  GET  /pmsi/form/{formId}/media/{assetId}?v=thumb|display|orig  (공개)
 */
@RestController
@RequestMapping("/pmsi/form/{formId}/media")
public class EgovMediaController {

    @javax.annotation.Resource(name = "formService")
    private FormService formService;

    @javax.annotation.Resource(name = "imageAssetService")
    private ImageAssetService imageAssetService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @PathVariable String formId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser String userId) throws Exception {
        FormVO form = formService.selectForm(formId);
        if (!form.getOwnerId().equals(userId)) {
            throw PmsiException.forbidden("form.forbidden", "본인 폼에만 이미지를 올릴 수 있습니다.");
        }
        if (!"DRAFT".equals(form.getStatus()) && !"ACTIVE".equals(form.getStatus())) {
            throw PmsiException.badRequest("form.not.editable", "이미지를 올릴 수 없는 상태입니다.");
        }
        if (idUnsafe(formId)) {
            throw PmsiException.badRequest("form.invalid", "잘못된 폼 ID입니다.");
        }
        return imageAssetService.store(formId, file);
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<org.springframework.core.io.Resource> get(
            @PathVariable String formId,
            @PathVariable String assetId,
            @RequestParam(value = "v", defaultValue = "display") String variant) throws Exception {
        if (idUnsafe(formId) || idUnsafe(assetId)) {
            throw PmsiException.badRequest("media.invalid", "잘못된 ID입니다.");
        }
        formService.selectForm(formId);
        byte[] bytes = imageAssetService.load(formId, assetId, variant);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_TYPE, "image/webp")
                .body(new ByteArrayResource(bytes));
    }

    private boolean idUnsafe(String id) {
        return id == null || id.isBlank()
                || id.contains("..") || id.contains("/") || id.contains("\\");
    }
}
