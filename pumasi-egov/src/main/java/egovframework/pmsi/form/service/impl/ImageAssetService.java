package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.cmm.storage.StorageClient;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 문항 이미지 에셋: thumb / display / orig 파생본을 WebP로 생성해 StorageClient에 저장.
 * (local 디스크 / S3 호환은 pmsi.storage.mode로 선택)
 */
@Service("imageAssetService")
public class ImageAssetService {

    static {
        ImageIO.scanForPlugins();
    }
    public static final String VARIANT_THUMB = "thumb";
    public static final String VARIANT_DISPLAY = "display";
    public static final String VARIANT_ORIG = "orig";

    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private static final int THUMB_MAX = 480;
    private static final int DISPLAY_MAX = 1280;
    private static final int ORIG_MAX = 2400;

    @Resource(name = "storageClient")
    private StorageClient storage;

    public Map<String, Object> store(String formId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw PmsiException.badRequest("file.empty", "파일이 비어 있습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw PmsiException.badRequest("file.too.large", "이미지는 8MB 이하여야 합니다.");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!ALLOWED.contains(contentType)) {
            throw PmsiException.badRequest("file.type",
                    "허용되지 않는 이미지 형식입니다: " + contentType);
        }

        BufferedImage source;
        try (InputStream in = file.getInputStream()) {
            source = ImageIO.read(in);
        }
        if (source == null) {
            throw PmsiException.badRequest("file.invalid", "이미지를 읽을 수 없습니다.");
        }

        String assetId = UUID.randomUUID().toString().replace("-", "");
        String prefix = formId + "/media/" + assetId + "/";

        storage.put(prefix + "thumb.webp", encodeVariant(source, THUMB_MAX, 0.80), "image/webp");
        storage.put(prefix + "display.webp", encodeVariant(source, DISPLAY_MAX, 0.82), "image/webp");
        storage.put(prefix + "orig.webp", encodeVariant(source, ORIG_MAX, 0.90), "image/webp");

        String base = "/pmsi/form/" + formId + "/media/" + assetId;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("assetId", assetId);
        out.put("url", base);
        out.put("thumbUrl", base + "?v=thumb");
        out.put("displayUrl", base + "?v=display");
        out.put("origUrl", base + "?v=orig");
        out.put("width", source.getWidth());
        out.put("height", source.getHeight());
        return out;
    }

    /** 파생본 바이트 로드. 없으면 404 예외 */
    public byte[] load(String formId, String assetId, String variant) throws IOException {
        String v = normalizeVariant(variant);
        byte[] bytes = storage.get(formId + "/media/" + assetId + "/" + v + ".webp");
        if (bytes == null) {
            throw PmsiException.notFound("media.notfound", "이미지를 찾을 수 없습니다.");
        }
        return bytes;
    }

    public static String normalizeVariant(String variant) {
        if (variant == null || variant.isBlank()) return VARIANT_DISPLAY;
        return switch (variant.toLowerCase()) {
            case VARIANT_THUMB, VARIANT_DISPLAY, VARIANT_ORIG -> variant.toLowerCase();
            default -> throw PmsiException.badRequest("media.variant",
                    "v는 thumb, display, orig 중 하나여야 합니다.");
        };
    }

    private byte[] encodeVariant(BufferedImage source, int maxLongEdge, double quality)
            throws IOException {
        int w = source.getWidth();
        int h = source.getHeight();
        int longEdge = Math.max(w, h);
        var builder = Thumbnails.of(source);
        if (longEdge > maxLongEdge) {
            if (w >= h) builder = builder.width(maxLongEdge);
            else builder = builder.height(maxLongEdge);
        } else {
            builder = builder.size(w, h);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        builder.outputFormat("webp")
                .outputQuality(quality)
                .toOutputStream(bos);
        return bos.toByteArray();
    }
}
