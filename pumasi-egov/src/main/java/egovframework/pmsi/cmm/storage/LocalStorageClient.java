package egovframework.pmsi.cmm.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 로컬 디스크 스토리지 (개발/단일 인스턴스용 기본값).
 */
@Component("storageClient")
@ConditionalOnProperty(name = "pmsi.storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalStorageClient implements StorageClient {

    @Value("${pmsi.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void put(String key, byte[] bytes, String contentType) throws IOException {
        Path dest = resolve(key);
        Files.createDirectories(dest.getParent());
        Files.write(dest, bytes);
    }

    @Override
    public byte[] get(String key) throws IOException {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) return null;
        return Files.readAllBytes(path);
    }

    @Override
    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    /** path traversal 방지: 정규화 후 루트 하위인지 확인 */
    private Path resolve(String key) {
        Path root = Path.of(uploadDir).toAbsolutePath().normalize();
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("잘못된 스토리지 키: " + key);
        }
        return path;
    }
}
