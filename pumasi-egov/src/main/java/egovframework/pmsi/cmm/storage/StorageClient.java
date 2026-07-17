package egovframework.pmsi.cmm.storage;

import java.io.IOException;

/**
 * 업로드 파일 스토리지 추상화.
 *
 * key 규약(기존 로컬 디스크 레이아웃과 동일):
 *   문항 이미지: {formId}/media/{assetId}/{variant}.webp
 *   응답 첨부:   {formId}/{fileId}
 *
 * 구현: local(기본, 개발용 디스크) / s3(S3 호환 오브젝트 스토리지).
 * pmsi.storage.mode 로 선택한다.
 */
public interface StorageClient {

    void put(String key, byte[] bytes, String contentType) throws IOException;

    /** 없으면 null */
    byte[] get(String key) throws IOException;

    boolean exists(String key);
}
