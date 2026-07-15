package egovframework.pmsi.result.service.impl;

/** 개별 응답 메타 행 (MyBatis 매핑). 실제 respondent_id는 담지 않는다(익명화). */
public class ResponseRow {
    private String responseId;
    private String anonLabel;
    private String qualityFlag;
    private String submittedAt;

    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }
    public String getAnonLabel() { return anonLabel; }
    public void setAnonLabel(String anonLabel) { this.anonLabel = anonLabel; }
    public String getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(String qualityFlag) { this.qualityFlag = qualityFlag; }
    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
}
