package egovframework.pmsi.result.service;

import java.util.List;
import java.util.Map;

/**
 * 결과 조회 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 *
 * D7: 결과 열람은 무료(크레딧 게이트 없음). 소유권만 검증.
 */
public interface ResultService {

    /** 질문별 차트 데이터(pass 응답만 집계) */
    List<Map<String, Object>> chartData(String formId, String userId) throws Exception;
}
