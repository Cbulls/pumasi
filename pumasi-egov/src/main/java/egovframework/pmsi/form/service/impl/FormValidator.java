package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.form.service.QuestionVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 질문 유효성 검증 — 순수 로직(프레임워크 무관, 결정론적).
 */
public class FormValidator {

    public static final Set<String> SUPPORTED_TYPES = Set.of(
            "SHORT_TEXT", "LONG_TEXT", "RADIO", "CHECKBOX", "DROPDOWN",
            "LINEAR_SCALE", "RATING", "DATE",
            "DESCRIPTION", "IMAGE", "FILE");

    /** 응답·비용 산출에서 제외되는 안내 블록 */
    public static final Set<String> CONTENT_TYPES = Set.of("DESCRIPTION", "IMAGE");

    private static final Set<String> CHOICE_TYPES = Set.of("RADIO", "CHECKBOX", "DROPDOWN");
    private static final Set<String> TEXT_TYPES = Set.of("SHORT_TEXT", "LONG_TEXT");
    /** 척도 범위(scaleMin/Max)를 쓰는 유형 */
    private static final Set<String> SCALE_TYPES = Set.of("LINEAR_SCALE", "RATING");

    public List<String> validate(QuestionVO q) {
        List<String> errors = new ArrayList<>();

        if (q.getTitle() == null || q.getTitle().isBlank()) {
            errors.add("질문 제목은 필수입니다.");
        }
        String type = q.getType();
        if (type == null || !SUPPORTED_TYPES.contains(type)) {
            errors.add("지원하지 않는 질문 유형입니다: " + type);
            return errors;
        }

        boolean isChoice = CHOICE_TYPES.contains(type);
        int optCount = q.getOptions() == null ? 0 : q.getOptions().size();

        if (CONTENT_TYPES.contains(type) || "FILE".equals(type)) {
            if (optCount > 0) errors.add(type + " 유형은 보기(options)를 가질 수 없습니다.");
            if (q.getMinSelect() != null || q.getMaxSelect() != null) {
                errors.add("선택 개수 제한은 CHECKBOX에서만 사용할 수 있습니다.");
            }
            if (q.getMinLength() != null || q.getMaxLength() != null) {
                errors.add("글자수 제한은 텍스트형에서만 사용할 수 있습니다.");
            }
            if (q.getScaleMin() != null || q.getScaleMax() != null) {
                errors.add("척도 범위는 LINEAR_SCALE/RATING에서만 사용할 수 있습니다.");
            }
        }

        if ("IMAGE".equals(type)) {
            if (q.getImageUrl() == null || q.getImageUrl().isBlank()) {
                errors.add("IMAGE 유형은 이미지 업로드가 필요합니다.");
            } else if (!isAllowedImageRef(q.getImageUrl())) {
                errors.add("IMAGE imageUrl 형식이 올바르지 않습니다.");
            }
        }
        if (q.getImageUrl() != null && !q.getImageUrl().isBlank()
                && !"IMAGE".equals(type)
                && !isAllowedImageRef(q.getImageUrl())) {
            errors.add("문항 이미지 URL 형식이 올바르지 않습니다.");
        }
        if ("DESCRIPTION".equals(type)) {
            // bodyHtml은 선택(제목만으로도 가능)
        }

        if (isChoice) {
            if (optCount < 2) {
                errors.add("선택형 질문은 보기가 2개 이상이어야 합니다.");
            }
        } else if (optCount > 0 && !CONTENT_TYPES.contains(type) && !"FILE".equals(type)) {
            errors.add(type + " 유형은 보기(options)를 가질 수 없습니다.");
        }

        if ("CHECKBOX".equals(type)) {
            Integer min = q.getMinSelect();
            Integer max = q.getMaxSelect();
            if (min != null && min < 0) errors.add("minSelect는 0 이상이어야 합니다.");
            if (max != null && max < 1) errors.add("maxSelect는 1 이상이어야 합니다.");
            if (min != null && max != null && min > max) {
                errors.add("minSelect는 maxSelect보다 클 수 없습니다.");
            }
            if (max != null && optCount > 0 && max > optCount) {
                errors.add("maxSelect는 보기 개수를 초과할 수 없습니다.");
            }
        } else if (q.getMinSelect() != null || q.getMaxSelect() != null) {
            if (!CONTENT_TYPES.contains(type) && !"FILE".equals(type)) {
                errors.add("선택 개수 제한은 CHECKBOX에서만 사용할 수 있습니다.");
            }
        }

        if (TEXT_TYPES.contains(type)) {
            Integer min = q.getMinLength();
            Integer max = q.getMaxLength();
            if (min != null && min < 0) errors.add("minLength는 0 이상이어야 합니다.");
            if (max != null && max < 1) errors.add("maxLength는 1 이상이어야 합니다.");
            if (min != null && max != null && min > max) {
                errors.add("minLength는 maxLength보다 클 수 없습니다.");
            }
        } else if (q.getMinLength() != null || q.getMaxLength() != null) {
            if (!CONTENT_TYPES.contains(type) && !"FILE".equals(type)) {
                errors.add("글자수 제한은 텍스트형에서만 사용할 수 있습니다.");
            }
        }

        if (SCALE_TYPES.contains(type)) {
            Integer smin = q.getScaleMin();
            Integer smax = q.getScaleMax();
            if (smin == null || smax == null) {
                errors.add(type + "는 scaleMin/scaleMax가 필요합니다.");
            } else if (smin >= smax) {
                errors.add("scaleMin은 scaleMax보다 작아야 합니다.");
            }
        } else if (q.getScaleMin() != null || q.getScaleMax() != null) {
            if (!CONTENT_TYPES.contains(type) && !"FILE".equals(type)) {
                errors.add("척도 범위는 LINEAR_SCALE/RATING에서만 사용할 수 있습니다.");
            }
        }

        if (q.getBranchRules() != null && !q.getBranchRules().isEmpty()) {
            if (!"RADIO".equals(type)) {
                errors.add("조건부 분기는 RADIO에서만 사용할 수 있습니다.");
            }
        }

        if (q.getAttentionAnswer() != null && !q.getAttentionAnswer().isBlank()) {
            if (!"RADIO".equals(type)) {
                errors.add("주의 문항 정답은 RADIO에서만 사용할 수 있습니다.");
            } else if (q.getOptions() == null || !q.getOptions().contains(q.getAttentionAnswer())) {
                errors.add("주의 문항 정답은 보기 중 하나여야 합니다.");
            }
        }

        return errors;
    }

    /** 분기 규칙의 sectionId가 유효한지(같은 폼, 현재보다 뒤 섹션) 검사 */
    public List<String> validateBranchRules(QuestionVO q, Map<String, Integer> sectionOrderById,
                                            int currentSectionOrder) {
        List<String> errors = new ArrayList<>();
        Map<String, String> rules = q.getBranchRules();
        if (rules == null || rules.isEmpty()) return errors;
        if (!"RADIO".equals(q.getType())) {
            errors.add("조건부 분기는 RADIO에서만 사용할 수 있습니다.");
            return errors;
        }
        for (Map.Entry<String, String> e : rules.entrySet()) {
            String target = e.getValue();
            if (target == null || target.isBlank()) {
                errors.add("분기 대상 섹션이 비어 있습니다: " + e.getKey());
                continue;
            }
            Integer order = sectionOrderById.get(target);
            if (order == null) {
                errors.add("존재하지 않는 섹션으로의 분기입니다: " + target);
            } else if (order <= currentSectionOrder) {
                errors.add("분기는 현재보다 뒤 섹션만 허용됩니다: " + e.getKey());
            }
        }
        return errors;
    }

    /** 미디어 에셋 기준 경로 또는 레거시 https URL */
    static boolean isAllowedImageRef(String ref) {
        String r = ref.trim();
        if (r.startsWith("/pmsi/form/") && r.contains("/media/")) {
            return !r.contains("..") && !r.contains("?");
        }
        return r.startsWith("https://") || r.startsWith("http://");
    }
}
