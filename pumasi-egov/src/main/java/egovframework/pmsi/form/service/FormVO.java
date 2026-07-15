package egovframework.pmsi.form.service;

/**
 * 폼 VO (가변 JavaBean — 표준 관례).
 *
 * status: DRAFT(작성중) → ACTIVE(게시) → CLOSED(마감)
 * costCredits: 응답 1건당 비용. publish 시점에 질문 구성으로 자동 산출(D4).
 */
public class FormVO {
    private String formId;
    private String ownerId;
    private String title;
    private String description;
    private String status;
    private int costCredits;
    private int maxResponses = 100;

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getCostCredits() { return costCredits; }
    public void setCostCredits(int costCredits) { this.costCredits = costCredits; }
    public int getMaxResponses() { return maxResponses; }
    public void setMaxResponses(int maxResponses) { this.maxResponses = maxResponses; }
}
