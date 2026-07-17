package egovframework.pmsi.form.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * 질문 VO.
 *
 * 지원 유형: SHORT_TEXT, LONG_TEXT, RADIO, CHECKBOX, DROPDOWN, LINEAR_SCALE,
 *           RATING, DATE, TIME, MULTIPLE_CHOICE_GRID, CHECKBOX_GRID,
 *           DESCRIPTION, IMAGE, FILE.
 */
public class QuestionVO {
    private String questionId;
    private String formId;
    private String sectionId;

    @NotBlank(message = "질문 유형은 필수입니다.")
    private String type;

    @NotBlank(message = "질문 제목은 필수입니다.")
    @Size(max = 500, message = "질문 제목은 500자 이하여야 합니다.")
    private String title;

    private boolean required;
    private int orderIndex;

    private List<String> options;

    /** 그리드 행 라벨 (MULTIPLE_CHOICE_GRID / CHECKBOX_GRID). 열은 options. */
    private List<String> rowLabels;

    private Integer minSelect;
    private Integer maxSelect;
    private Integer minLength;
    private Integer maxLength;
    private String  regex;
    private Integer scaleMin;
    private Integer scaleMax;

    /** DESCRIPTION 본문(HTML/텍스트) */
    private String bodyHtml;
    /** IMAGE 이미지 URL */
    private String imageUrl;
    /**
     * RADIO 분기 규칙: 보기 라벨 → sectionId, 특수 키 "_default".
     * DB에는 JSONB로 저장, API에서는 Map으로 주고받는다.
     */
    private Map<String, String> branchRules;

    /** RADIO 전용 주의 문항 정답. 설정 시 응답 불일치 → 품질 reject */
    private String attentionAnswer;

    /** RADIO/CHECKBOX: 기타 직접입력 허용 */
    private Boolean allowOther;
    /** 응답 화면에서만 보기 순서 셔플 */
    private Boolean shuffleOptions;

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public List<String> getRowLabels() { return rowLabels; }
    public void setRowLabels(List<String> rowLabels) { this.rowLabels = rowLabels; }
    public Integer getMinSelect() { return minSelect; }
    public void setMinSelect(Integer minSelect) { this.minSelect = minSelect; }
    public Integer getMaxSelect() { return maxSelect; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }
    public Integer getMinLength() { return minLength; }
    public void setMinLength(Integer minLength) { this.minLength = minLength; }
    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
    public String getRegex() { return regex; }
    public void setRegex(String regex) { this.regex = regex; }
    public Integer getScaleMin() { return scaleMin; }
    public void setScaleMin(Integer scaleMin) { this.scaleMin = scaleMin; }
    public Integer getScaleMax() { return scaleMax; }
    public void setScaleMax(Integer scaleMax) { this.scaleMax = scaleMax; }
    public String getBodyHtml() { return bodyHtml; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Map<String, String> getBranchRules() { return branchRules; }
    public void setBranchRules(Map<String, String> branchRules) { this.branchRules = branchRules; }
    public String getAttentionAnswer() { return attentionAnswer; }
    public void setAttentionAnswer(String attentionAnswer) { this.attentionAnswer = attentionAnswer; }
    public Boolean getAllowOther() { return allowOther; }
    public void setAllowOther(Boolean allowOther) { this.allowOther = allowOther; }
    public Boolean getShuffleOptions() { return shuffleOptions; }
    public void setShuffleOptions(Boolean shuffleOptions) { this.shuffleOptions = shuffleOptions; }
}
