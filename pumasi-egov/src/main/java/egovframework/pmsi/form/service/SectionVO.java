package egovframework.pmsi.form.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/** 폼 섹션(페이지) VO. */
public class SectionVO {
    private String sectionId;
    private String formId;

    @NotBlank(message = "섹션 제목은 필수입니다.")
    @Size(max = 300, message = "섹션 제목은 300자 이하여야 합니다.")
    private String title;

    private int orderIndex;
    private List<QuestionVO> questions = new ArrayList<>();

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }
    public String getFormId() { return formId; }
    public void setFormId(String formId) { this.formId = formId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public List<QuestionVO> getQuestions() { return questions; }
    public void setQuestions(List<QuestionVO> questions) { this.questions = questions; }
}
