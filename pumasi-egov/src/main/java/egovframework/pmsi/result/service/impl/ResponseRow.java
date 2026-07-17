package egovframework.pmsi.result.service.impl;

/**
 * 개별 응답 메타 행.
 * respondentId는 서비스 내부에서 언락 판정에만 쓰고 API 응답에는 넣지 않는다.
 */
public class ResponseRow {
    private String responseId;
    private String respondentId;
    private String anonLabel;
    private String qualityFlag;
    private String submittedAt;

    public String getResponseId() { return responseId; }
    public void setResponseId(String responseId) { this.responseId = responseId; }
    public String getRespondentId() { return respondentId; }
    public void setRespondentId(String respondentId) { this.respondentId = respondentId; }
    public String getAnonLabel() { return anonLabel; }
    public void setAnonLabel(String anonLabel) { this.anonLabel = anonLabel; }
    public String getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(String qualityFlag) { this.qualityFlag = qualityFlag; }
    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
}
