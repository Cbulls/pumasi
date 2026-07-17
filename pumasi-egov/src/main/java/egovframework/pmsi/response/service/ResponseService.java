package egovframework.pmsi.response.service;

/**
 * 응답 수집 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 */
public interface ResponseService {

    /**
     * 응답 시작 기록. 서버가 시작 시각을 저장해 제출 시 소요시간을 서버에서 계산한다.
     * (클라이언트 elapsedSeconds 신뢰 제거)
     */
    void start(String formId, String respondentId) throws Exception;

    /**
     * 응답 제출.
     * 유효성 검증 → 저장 → quality 판정 → pass면 크레딧 정산(1인 1회 UNIQUE).
     */
    SubmitResultVO submit(String formId, String respondentId, SubmitRequestVO req) throws Exception;

    /**
     * HOLD 응답 검토(소유자). decision=pass면 소급 정산(멱등), reject면 플래그만 변경.
     */
    void review(String formId, String responseId, String ownerId, String decision) throws Exception;
}
