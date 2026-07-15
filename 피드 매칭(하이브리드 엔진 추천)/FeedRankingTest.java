import java.util.*;

/**
 * 하이브리드 피드 점수·정렬 TDD (D1: 풀 기본 + 1:1 매칭 레이어).
 *
 * 점수 = 풀 기본 점수 + 1:1 매칭 부스트 + 공정성 보정.
 *
 * 규칙:
 *  R1. ★풀 기본 점수★ 보상 크레딧이 높을수록 높은 점수(응답 유인).
 *  R2. ★1:1 매칭 부스트★ "나에게 응답해준 사람"의 설문에 가산점(호혜성 — D1 핵심).
 *  R3. ★공정성 보정★ 누적 응답 적게 받은 설문에 가산점(신규/소외 설문 노출 — 빈익빈 방지).
 *  R4. ★신선도★ 최근 생성 설문에 소폭 가산(오래된 설문이 영원히 상위 점유 방지).
 *  R5. 정렬: 최종 점수 내림차순.
 *  R6. ★다양성★ 같은 제작자 설문이 연속으로 N개 초과해 상위 점유하지 않게(분산).
 *  R7. 페이지네이션: 상위 K개만 반환.
 */
public class FeedRankingTest {
    static int pass=0, fail=0;
    static void check(String n, boolean c){ if(c){pass++;System.out.println("  PASS "+n);}else{fail++;System.out.println("  FAIL "+n);} }

    public static void main(String[] a){
        // 매칭 컨텍스트: user1에게 응답해준 사람 = {ownerB} (그의 설문이 부스트 대상)
        MatchContext ctx = new MatchContext("user1", Set.of("ownerB"), 1000L);
        FeedRanker ranker = new FeedRanker();

        System.out.println("== R1: 풀 기본 점수(보상 높을수록) ==");
        {
            var low = card("low","ownerX").reward(2).build();
            var high = card("high","ownerX").reward(8).build();
            double sLow = ranker.score(low, ctx);
            double sHigh = ranker.score(high, ctx);
            check("높은 보상 > 낮은 보상", sHigh > sLow);
        }

        System.out.println("== R2: 1:1 매칭 부스트 ==");
        {
            // 동일 조건인데 ownerB(나에게 응답해준 사람)의 설문이 더 높아야
            var fromB = card("b","ownerB").reward(4).build();
            var fromX = card("x","ownerX").reward(4).build();
            double sB = ranker.score(fromB, ctx);
            double sX = ranker.score(fromX, ctx);
            check("호혜 대상 부스트 적용", sB > sX);
        }

        System.out.println("== R3: 공정성 보정(응답 적을수록) ==");
        {
            var fresh = card("fresh","ownerX").reward(4).totalReceived(0).build();
            var popular = card("pop","ownerX").reward(4).totalReceived(1000).build();
            check("응답 적은 설문 > 많이 받은 설문",
                ranker.score(fresh, ctx) > ranker.score(popular, ctx));
        }

        System.out.println("== R4: 신선도 ==");
        {
            var recent = card("recent","ownerX").reward(4).createdAt(990).build();   // now=1000
            var old = card("old","ownerX").reward(4).createdAt(100).build();
            check("최근 생성 > 오래된", ranker.score(recent, ctx) > ranker.score(old, ctx));
        }

        System.out.println("== R5: 정렬(내림차순) ==");
        {
            var cards = List.of(
                card("c1","ownerX").reward(2).build(),
                card("c2","ownerB").reward(4).build(),   // 부스트
                card("c3","ownerX").reward(8).build());
            var ranked = ranker.rank(cards, ctx, 10, 99);
            // 점수 내림차순인지
            boolean desc = true;
            for (int i=1;i<ranked.size();i++)
                if (ranker.score(ranked.get(i-1), ctx) < ranker.score(ranked.get(i), ctx)) desc=false;
            check("점수 내림차순 정렬", desc);
        }

        System.out.println("== R6: 다양성(같은 제작자 연속 제한) ==");
        {
            // ownerY가 설문 5개를 다 상위 점유하려는 상황
            var cards = new ArrayList<SurveyCard>();
            for (int i=0;i<5;i++) cards.add(card("y"+i,"ownerY").reward(8).build());
            cards.add(card("z","ownerZ").reward(3).build());   // 낮지만 다른 제작자
            var ranked = ranker.rank(cards, ctx, 10, 2);       // maxPerOwner=2
            long yCount = ranked.stream().filter(c -> c.ownerId.equals("ownerY")).count();
            check("같은 제작자 최대 2개", yCount <= 2);
            check("다른 제작자도 노출됨", ranked.stream().anyMatch(c -> c.ownerId.equals("ownerZ")));
        }

        System.out.println("== R7: 상위 K개 ==");
        {
            var cards = new ArrayList<SurveyCard>();
            for (int i=0;i<20;i++) cards.add(card("k"+i,"o"+i).reward(4).build());
            var ranked = ranker.rank(cards, ctx, 5, 99);
            check("상위 5개만 반환", ranked.size()==5);
        }

        System.out.println("\n결과: "+pass+" pass / "+fail+" fail");
        if(fail>0) System.exit(1);
    }
    static SurveyBuilder card(String id, String owner){ return new SurveyBuilder(id).owner(owner); }
}
