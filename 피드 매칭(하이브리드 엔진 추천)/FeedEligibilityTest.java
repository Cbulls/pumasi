import java.util.*;

/**
 * 피드 자격 필터링 TDD. 이 응답자에게 "보여줄 수 있는" 설문만 거른다.
 *
 * 규칙:
 *  E1. ★자기 설문 제외★ 내가 만든 설문은 추천 안 함.
 *  E2. ★이미 응답 제외★ 1인 1회 — 이미 응답한 설문 제외.
 *  E3. ★비활성 제외★ ACTIVE 상태가 아닌 설문(DRAFT/CLOSED) 제외.
 *  E4. ★예치금 소진 제외★ escrow 잔액이 1회 보상 미만이면 제외(응답해도 보상 불가).
 *  E5. ★마감 제외★ 마감일 지났거나 목표 응답 수 채운 설문 제외.
 *  E6. ★타깃 조건★ 설문에 타깃 필터가 있으면 응답자가 조건에 맞아야(예: 연령대).
 *  E7. 나머지(자격 있는 설문)는 통과.
 */
public class FeedEligibilityTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    public static void main(String[] args) {
        FeedEligibility elig = new FeedEligibility();

        // 응답자: user1, 30대. 이미 응답: {f_done}
        Viewer viewer = new Viewer("user1", "30s", Set.of("f_done"));

        System.out.println("== E1: 자기 설문 제외 ==");
        check("내 설문 → 제외",
            !elig.isEligible(survey("f1").owner("user1").build(), viewer));
        check("남의 설문 → 통과",
            elig.isEligible(survey("f2").owner("other").build(), viewer));

        System.out.println("== E2: 이미 응답 제외 ==");
        check("응답한 설문 → 제외",
            !elig.isEligible(survey("f_done").owner("other").build(), viewer));

        System.out.println("== E3: 비활성 제외 ==");
        check("DRAFT → 제외",
            !elig.isEligible(survey("f3").owner("other").status("DRAFT").build(), viewer));
        check("CLOSED → 제외",
            !elig.isEligible(survey("f4").owner("other").status("CLOSED").build(), viewer));
        check("ACTIVE → 통과",
            elig.isEligible(survey("f5").owner("other").status("ACTIVE").build(), viewer));

        System.out.println("== E4: 예치금 소진 제외 ==");
        check("escrow < 1회 보상 → 제외",
            !elig.isEligible(survey("f6").owner("other").escrow(0).rewardPerResponse(4).build(), viewer));
        check("escrow >= 1회 보상 → 통과",
            elig.isEligible(survey("f7").owner("other").escrow(4).rewardPerResponse(4).build(), viewer));

        System.out.println("== E5: 마감 제외 ==");
        check("목표 응답 수 채움 → 제외",
            !elig.isEligible(survey("f8").owner("other").maxResponses(100).currentResponses(100).build(), viewer));
        check("마감일 지남 → 제외",
            !elig.isEligible(survey("f9").owner("other").deadlinePassed(true).build(), viewer));

        System.out.println("== E6: 타깃 조건 ==");
        check("타깃 30s, 응답자 30s → 통과",
            elig.isEligible(survey("f10").owner("other").targetAge("30s").build(), viewer));
        check("타깃 20s, 응답자 30s → 제외",
            !elig.isEligible(survey("f11").owner("other").targetAge("20s").build(), viewer));
        check("타깃 없음 → 통과(전체 대상)",
            elig.isEligible(survey("f12").owner("other").build(), viewer));

        System.out.println("== E7: 일괄 필터 ==");
        var all = List.of(
            survey("f1").owner("user1").build(),          // 자기
            survey("f_done").owner("other").build(),       // 응답함
            survey("ok1").owner("other").build(),          // 통과
            survey("ok2").owner("other").build());         // 통과
        var eligible = elig.filterEligible(all, viewer);
        check("4개 중 2개만 자격", eligible.size() == 2);

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }

    static SurveyBuilder survey(String id) { return new SurveyBuilder(id); }
}
