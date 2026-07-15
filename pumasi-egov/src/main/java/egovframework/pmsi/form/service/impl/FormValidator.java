package egovframework.pmsi.form.service.impl;

import egovframework.pmsi.form.service.QuestionVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 질문 유효성 검증 — 순수 로직(프레임워크 무관, 결정론적).
 *
 * 폼 빌더의 핵심 복잡도: "질문 유형마다 허용 옵션이 다르다".
 *   - 선택형(RADIO/CHECKBOX)만 옵션을 가진다.
 *   - CHECKBOX만 최소/최대 선택 개수를 가진다.
 *   - 텍스트형만 글자수 제한을 가진다.
 *   - LINEAR_SCALE만 척도 범위를 가진다.
 *
 * 마스터 설계문서 §3.3 의 유형별 검증 규칙을 스켈레톤 지원 유형으로 구현.
 */
public class FormValidator {

    public static final Set<String> SUPPORTED_TYPES =
            Set.of("SHORT_TEXT", "LONG_TEXT", "RADIO", "CHECKBOX", "LINEAR_SCALE");

    private static final Set<String> CHOICE_TYPES = Set.of("RADIO", "CHECKBOX");
    private static final Set<String> TEXT_TYPES = Set.of("SHORT_TEXT", "LONG_TEXT");

    /** @return 오류 메시지 목록(비어 있으면 유효) */
    public List<String> validate(QuestionVO q) {
        List<String> errors = new ArrayList<>();

        if (q.getTitle() == null || q.getTitle().isBlank()) {
            errors.add("질문 제목은 필수입니다.");
        }
        String type = q.getType();
        if (type == null || !SUPPORTED_TYPES.contains(type)) {
            errors.add("지원하지 않는 질문 유형입니다: " + type);
            return errors;   // 유형 미확정이면 이후 검증 무의미
        }

        boolean isChoice = CHOICE_TYPES.contains(type);
        int optCount = q.getOptions() == null ? 0 : q.getOptions().size();

        // 선택형만 옵션 허용, 그 외는 옵션 금지
        if (isChoice) {
            if (optCount < 2) {
                errors.add("선택형 질문은 보기가 2개 이상이어야 합니다.");
            }
        } else if (optCount > 0) {
            errors.add(type + " 유형은 보기(options)를 가질 수 없습니다.");
        }

        // CHECKBOX만 최소/최대 선택 개수 허용
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
            errors.add("선택 개수 제한은 CHECKBOX에서만 사용할 수 있습니다.");
        }

        // 텍스트형만 글자수 제한 허용
        if (TEXT_TYPES.contains(type)) {
            Integer min = q.getMinLength();
            Integer max = q.getMaxLength();
            if (min != null && min < 0) errors.add("minLength는 0 이상이어야 합니다.");
            if (max != null && max < 1) errors.add("maxLength는 1 이상이어야 합니다.");
            if (min != null && max != null && min > max) {
                errors.add("minLength는 maxLength보다 클 수 없습니다.");
            }
        } else if (q.getMinLength() != null || q.getMaxLength() != null) {
            errors.add("글자수 제한은 텍스트형에서만 사용할 수 있습니다.");
        }

        // LINEAR_SCALE만 척도 범위 허용/필수
        if ("LINEAR_SCALE".equals(type)) {
            Integer smin = q.getScaleMin();
            Integer smax = q.getScaleMax();
            if (smin == null || smax == null) {
                errors.add("LINEAR_SCALE는 scaleMin/scaleMax가 필요합니다.");
            } else if (smin >= smax) {
                errors.add("scaleMin은 scaleMax보다 작아야 합니다.");
            }
        } else if (q.getScaleMin() != null || q.getScaleMax() != null) {
            errors.add("척도 범위는 LINEAR_SCALE에서만 사용할 수 있습니다.");
        }

        return errors;
    }
}
