package egovframework.pmsi.result.service.impl;

/** 집계 입력용 응답-답변 조인 행 (MyBatis 매핑). public: MyBatis 리플렉션 인스턴스화 안정성. */
public class AnswerRow {
    private String responseId;
    private String questionId;
    private String value;

    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
