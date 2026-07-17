package egovframework.pmsi.cmm.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;

/**
 * S3 호환 오브젝트 스토리지 (pmsi.storage.mode=s3).
 *
 * endpoint를 지정하면 MinIO 등 S3 호환 스토리지, 비우면 AWS S3.
 * 자격증명은 AWS SDK 기본 체인(env AWS_ACCESS_KEY_ID 등)을 사용한다.
 */
@Component("storageClient")
@ConditionalOnProperty(name = "pmsi.storage.mode", havingValue = "s3")
public class S3StorageClient implements StorageClient {

    @Value("${pmsi.storage.s3.bucket:}")
    private String bucket;

    @Value("${pmsi.storage.s3.region:ap-northeast-2}")
    private String region;

    @Value("${pmsi.storage.s3.endpoint:}")
    private String endpoint;

    private S3Client s3;

    @PostConstruct
    void init() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("pmsi.storage.mode=s3 이면 pmsi.storage.s3.bucket 이 필요합니다.");
        }
        var builder = S3Client.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
        }
        this.s3 = builder.build();
    }

    @PreDestroy
    void close() {
        if (s3 != null) s3.close();
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) throws IOException {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(bytes));
    }

    @Override
    public byte[] get(String key) throws IOException {
        try {
            ResponseBytes<GetObjectResponse> res = s3.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            return res.asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 404) return false;
            throw e;
        }
    }
}
