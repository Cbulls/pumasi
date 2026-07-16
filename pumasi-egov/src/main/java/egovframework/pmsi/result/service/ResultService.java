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

    /**
     * 개별 응답 표(구글폼 "개별 보기" 유사). 소유자만.
     * 반환: { questions:[{questionId,title,type}], rows:[{anonLabel,qualityFlag,submittedAt,answers:{qid:"값"}}] }
     * 실제 respondent_id는 포함하지 않는다(익명 라벨만).
     */
    Map<String, Object> responseTable(String formId, String userId) throws Exception;

    /** 개별 응답 CSV(UTF-8 BOM). 소유자만. */
    String exportCsv(String formId, String userId) throws Exception;
}
