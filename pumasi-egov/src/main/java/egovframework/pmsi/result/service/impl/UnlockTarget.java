package egovframework.pmsi.result.service.impl;

/** 잠긴 응답을 열기 위해 상대 설문으로 안내할 때 쓰는 최소 메타. */
public class UnlockTarget {
    private String formId;
    private String title;
    private String shareToken;

    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }
}
