import java.util.*;

/** 내보내기용 질문 (라벨 매핑 포함) */
class ExQuestion {
    String id, title, type;
    Map<String, String> optionLabels = Map.of();   // 옵션ID → 라벨
    List<String> optionOrder = List.of();           // 펼치기 열 순서
    static ExQuestion choice(String id, String title, String type,
                             Map<String, String> labels, List<String> order) {
        ExQuestion q = new ExQuestion();
        q.id = id; q.title = title; q.type = type;
        q.optionLabels = labels; q.optionOrder = order; return q;
    }
}

/** 내보내기용 응답 */
class ExResponse {
    String submittedAt, qualityFlag;
    Map<String, List<String>> answers;
    static ExResponse of(String submittedAt, String flag, Map<String, List<String>> answers) {
        ExResponse r = new ExResponse();
        r.submittedAt = submittedAt; r.qualityFlag = flag; r.answers = answers; return r;
    }
}

/**
 * 결과 내보내기 변환기. 응답을 행렬(1행=1응답)로.
 *
 * 핵심 결정:
 *  - 집계(그래프)는 pass만, 내보내기(raw)는 전부(quality_flag로 구분) — 용도가 다르다.
 *  - 체크박스: 합치기 모드(쉼표, 사람용) vs 펼치기 모드(보기별 0/1 열, 분석용).
 *  - 출력은 옵션 ID가 아니라 라벨(사람이 읽음).
 *  - CSV는 쉼표/따옴표/줄바꿈 이스케이프(RFC 4180).
 *
 * POI(.xlsx)는 이 행렬을 셀에 쓰기만 하면 됨(여기선 행렬 + CSV까지).
 */
public class ResultExporter {

    /**
     * 응답을 행렬로. 첫 행은 헤더.
     * @param expand 체크박스를 보기별 0/1 열로 펼칠지(true) 쉼표 합칠지(false)
     */
    List<List<String>> toMatrix(List<ExQuestion> questions, List<ExResponse> responses,
                                boolean expand) {
        List<List<String>> rows = new ArrayList<>();

        // 헤더
        List<String> header = new ArrayList<>(List.of("제출시각", "quality_flag"));
        for (ExQuestion q : questions) {
            if (expand && "CHECKBOX".equals(q.type)) {
                for (String opt : q.optionOrder) {
                    header.add(q.title + "_" + q.optionLabels.getOrDefault(opt, opt));
                }
            } else {
                header.add(q.title);
            }
        }
        rows.add(header);

        // 데이터 행
        for (ExResponse r : responses) {
            List<String> row = new ArrayList<>();
            row.add(r.submittedAt);
            row.add(r.qualityFlag);
            for (ExQuestion q : questions) {
                List<String> ans = r.answers.get(q.id);
                if (expand && "CHECKBOX".equals(q.type)) {
                    // 보기별 0/1
                    for (String opt : q.optionOrder) {
                        boolean selected = ans != null && ans.contains(opt);
                        row.add(selected ? "1" : "0");
                    }
                } else {
                    row.add(formatAnswer(q, ans));
                }
            }
            rows.add(row);
        }
        return rows;
    }

    /** 답을 라벨로. 선택형은 라벨 매핑, 복수는 쉼표 합침. 무응답은 빈 문자열. */
    private String formatAnswer(ExQuestion q, List<String> ans) {
        if (ans == null || ans.isEmpty()) return "";   // X5: 무응답
        boolean isChoice = q.type.equals("RADIO") || q.type.equals("DROPDOWN")
            || q.type.equals("CHECKBOX");
        if (isChoice) {
            List<String> labels = new ArrayList<>();
            for (String a : ans) labels.add(q.optionLabels.getOrDefault(a, a));   // X7: 라벨
            return String.join(", ", labels);   // X3: 쉼표 합침
        }
        return ans.get(0);   // 텍스트/배율은 값 그대로
    }

    /** CSV 필드 이스케이프 (RFC 4180). */
    String toCsvField(String value) {
        if (value == null) return "";
        boolean needsQuote = value.contains(",") || value.contains("\"")
            || value.contains("\n") || value.contains("\r");
        if (!needsQuote) return value;
        String escaped = value.replace("\"", "\"\"");   // 내부 따옴표 → ""
        return "\"" + escaped + "\"";
    }

    /** 행렬 → CSV 문자열 */
    String toCsv(List<ExQuestion> questions, List<ExResponse> responses, boolean expand) {
        List<List<String>> matrix = toMatrix(questions, responses, expand);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.size(); i++) {
            List<String> row = matrix.get(i);
            List<String> escaped = new ArrayList<>();
            for (String cell : row) escaped.add(toCsvField(cell));
            sb.append(String.join(",", escaped));
            if (i < matrix.size() - 1) sb.append("\n");
        }
        return sb.toString();
    }
}
