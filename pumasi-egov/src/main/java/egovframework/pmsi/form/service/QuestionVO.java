package egovframework.pmsi.form.service;

import java.util.List;

/**
 * 질문 VO.
 *
 * 지원 유형(스켈레톤): SHORT_TEXT, LONG_TEXT, RADIO, CHECKBOX, LINEAR_SCALE.
 * 유형마다 허용 옵션이 다른 게 폼 빌더의 핵심 복잡도 → FormValidator가 검증.
 */
public class QuestionVO {
    private String questionId;
    private String formId;
    private String sectionId;
    private String type;
    private String title;
    private boolean required;
    private int orderIndex;

    private List<String> options;   // 선택형(RADIO/CHECKBOX) 보기 라벨

    private Integer minSelect;      // CHECKBOX 최소 선택 개수
    private Integer maxSelect;      // CHECKBOX 최대 선택 개수
    private Integer minLength;      // 텍스트 최소 글자수
    private Integer maxLength;      // 텍스트 최대 글자수
    private String  regex;          // 단답형 정규식
    private Integer scaleMin;       // LINEAR_SCALE 최소값
    private Integer scaleMax;       // LINEAR_SCALE 최대값

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
}
