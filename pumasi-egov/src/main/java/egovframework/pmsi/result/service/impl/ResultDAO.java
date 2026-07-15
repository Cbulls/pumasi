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
}
