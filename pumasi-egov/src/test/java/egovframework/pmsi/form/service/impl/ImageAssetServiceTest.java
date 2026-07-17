package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.cmm.storage.LocalStorageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageAssetServiceTest {

    @TempDir
    Path temp;

    @Test
    void storeCreatesWebpVariants() throws Exception {
        ImageIO.scanForPlugins();
        BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 800, 600);
        g.dispose();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, "png", bos));

        LocalStorageClient storage = new LocalStorageClient();
        var dirField = LocalStorageClient.class.getDeclaredField("uploadDir");
        dirField.setAccessible(true);
        dirField.set(storage, temp.toString());

        ImageAssetService svc = new ImageAssetService();
        var field = ImageAssetService.class.getDeclaredField("storage");
        field.setAccessible(true);
        field.set(svc, storage);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", bos.toByteArray());

        var result = svc.store("formtest1", file);
        assertNotNull(result.get("assetId"));
        String assetId = (String) result.get("assetId");
        assertEquals("/pmsi/form/formtest1/media/" + assetId, result.get("url"));

        assertTrue(Files.isRegularFile(temp.resolve("formtest1/media/" + assetId + "/thumb.webp")));
        assertTrue(Files.isRegularFile(temp.resolve("formtest1/media/" + assetId + "/display.webp")));
        assertTrue(Files.isRegularFile(temp.resolve("formtest1/media/" + assetId + "/orig.webp")));
        assertTrue(Files.size(temp.resolve("formtest1/media/" + assetId + "/thumb.webp")) > 0);

        byte[] thumb = svc.load("formtest1", assetId, "thumb");
        assertNotNull(thumb);
        assertTrue(thumb.length > 0);
    }
}
