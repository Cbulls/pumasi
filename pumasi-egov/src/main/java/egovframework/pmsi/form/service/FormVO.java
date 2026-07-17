package egovframework.pmsi.form.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * 폼 VO (가변 JavaBean — 표준 관례).
 *
 * status: DRAFT(작성중) → ACTIVE(게시) → CLOSED(마감)
 * costCredits: 응답 1건당 비용. publish 시점에 질문 구성으로 자동 산출(D4).
 * closesAt: 마감일시(NULL이면 기한 없음).
 * shareToken: 공개 미리보기용 토큰(publish 시 발급).
 */
public class FormVO {
    private String formId;
    private String ownerId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 300, message = "제목은 300자 이하여야 합니다.")
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.")
    private String description;

    private String status;
    private int costCredits;

    @Min(value = 1, message = "최대 응답 수는 1 이상이어야 합니다.")
    @Max(value = 100000, message = "최대 응답 수가 너무 큽니다.")
    private int maxResponses = 100;

    private OffsetDateTime closesAt;
    private String shareToken;

    @Size(max = 500, message = "완료 메시지는 500자 이하여야 합니다.")
    private String confirmationMessage;

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
    public OffsetDateTime getClosesAt() { return closesAt; }
    public void setClosesAt(OffsetDateTime closesAt) { this.closesAt = closesAt; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
    public String getConfirmationMessage() { return confirmationMessage; }
    public void setConfirmationMessage(String confirmationMessage) {
        this.confirmationMessage = confirmationMessage;
    }
}
