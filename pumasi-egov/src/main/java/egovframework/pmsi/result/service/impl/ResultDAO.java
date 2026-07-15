package egovframework.pmsi.result.service.impl;

import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 결과 DAO — EgovAbstractMapper 상속(MyBatis).
 *
 * 집계는 pass 응답만 조회한다(그래프 왜곡 방지). 내보내기(전체)는 스켈레톤 범위 밖.
 */
@Repository("resultDAO")
public class ResultDAO extends EgovAbstractMapper {

    private static final String NS = "resultMapper.";

    /** pass 응답의 (responseId, questionId, value) 행 목록 */
    public List<AnswerRow> selectPassAnswers(String formId) {
        return getSqlSession().selectList(NS + "selectPassAnswers", formId);
    }

    /** 개별 응답 뷰용: 전체 응답 메타(익명 라벨/품질/제출시각) */
    public List<ResponseRow> selectResponses(String formId) {
        return getSqlSession().selectList(NS + "selectResponses", formId);
    }

    /** 개별 응답 뷰용: 전체 quality의 답변 행 */
    public List<AnswerRow> selectAllAnswers(String formId) {
        return getSqlSession().selectList(NS + "selectAllAnswers", formId);
    }
}
