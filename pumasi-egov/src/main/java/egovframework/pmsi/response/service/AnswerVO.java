package egovframework.pmsi.response.service;

import java.util.List;

/**
 * 응답 1문항.
 *
 * values: 다중선택(CHECKBOX)은 여러 값, 단일선택/텍스트/척도는 한 값.
 *   - 선택형: 보기 라벨
 *   - 척도  : 숫자 문자열
 *   - 텍스트: 자유 입력
 */
public class AnswerVO {
    private String questionId;
    private List<String> values;

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }
}
