import java.util.*;

/**
 * 결과 내보내기 TDD. 응답을 행렬(1행=1응답)로 변환. POI/CSV는 이 행렬을 쓰는 도구일 뿐,
 * 핵심은 변환 로직.
 *
 * 규칙:
 *  X1. 헤더 = [제출시각, quality_flag, 질문1 제목, 질문2 제목, ...].
 *  X2. 1응답 = 1행. 각 질문 칸에 그 응답의 답.
 *  X3. ★체크박스 합치기 모드★ 여러 선택을 쉼표로 합침("a, c"). (사람이 읽기 좋음)
 *  X4. ★체크박스 펼치기 모드★ 보기별 0/1 열로 분해(질문_보기 헤더). (분석용)
 *  X5. 무응답 칸은 빈 문자열.
 *  X6. ★CSV 이스케이프★ 값에 쉼표/따옴표/줄바꿈 있으면 큰따옴표로 감싸고 내부 따옴표는 "".
 *  X7. 옵션 ID가 아니라 보기 라벨로 출력(사람이 읽는 건 라벨).
 *  X8. reject/hold 응답도 내보내기엔 포함(원천 데이터는 전부, quality_flag로 구분).
 *      — 집계(그래프)는 pass만, 내보내기(raw)는 전부. 용도가 다르다.
 */
public class ExportTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    public static void main(String[] args) {
        ResultExporter ex = new ResultExporter();

        // 폼: q1(RADIO, 보기 a=좋음/b=나쁨), q2(CHECKBOX, x=빨강/y=파랑/z=초록)
        var questions = List.of(
            ExQuestion.choice("q1", "만족도", "RADIO",
                Map.of("a", "좋음", "b", "나쁨"), List.of("a", "b")),
            ExQuestion.choice("q2", "선호색", "CHECKBOX",
                Map.of("x", "빨강", "y", "파랑", "z", "초록"), List.of("x", "y", "z")));

        System.out.println("== X1/X2/X3: 헤더 + 체크박스 합치기 ==");
        {
            var responses = List.of(
                ExResponse.of("2026-06-01T10:00", "pass",
                    Map.of("q1", List.of("a"), "q2", List.of("x", "z"))));
            var rows = ex.toMatrix(questions, responses, false);   // combineMode
            var header = rows.get(0);
            check("헤더 시작 = 제출시각", header.get(0).equals("제출시각"));
            check("헤더[1] = quality_flag", header.get(1).equals("quality_flag"));
            check("헤더에 질문 제목", header.contains("만족도") && header.contains("선호색"));
            var row = rows.get(1);
            check("q1 라벨 출력(좋음)", row.contains("좋음"));   // X7
            check("체크박스 쉼표 합침(빨강, 초록)", row.contains("빨강, 초록"));  // X3
        }

        System.out.println("== X4: 체크박스 펼치기 모드 ==");
        {
            var responses = List.of(
                ExResponse.of("2026-06-01T10:00", "pass",
                    Map.of("q1", List.of("a"), "q2", List.of("x", "z"))));
            var rows = ex.toMatrix(questions, responses, true);    // expandMode
            var header = rows.get(0);
            // 체크박스가 보기별 열로 분해: "선호색_빨강", "선호색_파랑", "선호색_초록"
            check("펼친 헤더 존재", header.contains("선호색_빨강") && header.contains("선호색_초록"));
            var row = rows.get(1);
            int idxRed = header.indexOf("선호색_빨강");
            int idxBlue = header.indexOf("선호색_파랑");
            int idxGreen = header.indexOf("선호색_초록");
            check("빨강 선택=1", row.get(idxRed).equals("1"));
            check("파랑 미선택=0", row.get(idxBlue).equals("0"));
            check("초록 선택=1", row.get(idxGreen).equals("1"));
        }

        System.out.println("== X5/X8: 무응답 + 불성실 포함 ==");
        {
            var responses = List.of(
                ExResponse.of("t1", "pass", Map.of("q1", List.of("a"))),   // q2 무응답
                ExResponse.of("t2", "reject", Map.of("q1", List.of("b"), "q2", List.of("y"))));
            var rows = ex.toMatrix(questions, responses, false);
            check("2개 응답 모두 포함(reject도)", rows.size() == 3);  // 헤더 + 2행, X8
            // 첫 응답의 q2(선호색) 칸은 빈 문자열
            var header = rows.get(0);
            int idxColor = header.indexOf("선호색");
            check("무응답 칸 빈 문자열", rows.get(1).get(idxColor).isEmpty());  // X5
            check("reject 행의 flag 표시", rows.get(2).get(1).equals("reject"));
        }

        System.out.println("== X6: CSV 이스케이프 ==");
        {
            check("쉼표 포함 → 따옴표 감쌈",
                ex.toCsvField("a, b").equals("\"a, b\""));
            check("따옴표 포함 → 이스케이프",
                ex.toCsvField("say \"hi\"").equals("\"say \"\"hi\"\"\""));
            check("줄바꿈 포함 → 따옴표 감쌈",
                ex.toCsvField("line1\nline2").equals("\"line1\nline2\""));
            check("일반 텍스트 → 그대로",
                ex.toCsvField("hello").equals("hello"));
        }

        System.out.println("== CSV 전체 조립 ==");
        {
            var responses = List.of(
                ExResponse.of("t1", "pass", Map.of("q1", List.of("a"), "q2", List.of("x"))));
            String csv = ex.toCsv(questions, responses, false);
            check("CSV 줄 수 = 2(헤더+1)", csv.split("\n").length == 2);
            check("헤더 줄에 제목 포함", csv.split("\n")[0].contains("만족도"));
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }
}
