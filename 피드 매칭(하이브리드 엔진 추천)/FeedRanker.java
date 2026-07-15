import java.util.*;

/** 매칭 컨텍스트: 이 응답자에게 응답해준 사람들(호혜 부스트 대상) */
class MatchContext {
    String viewerId;
    Set<String> reciprocalOwners;   // 나에게 응답해준 사람들의 userId
    long nowEpoch;
    MatchContext(String viewerId, Set<String> reciprocalOwners, long nowEpoch) {
        this.viewerId = viewerId; this.reciprocalOwners = reciprocalOwners; this.nowEpoch = nowEpoch;
    }
}

/**
 * 하이브리드 피드 랭커 (D1: 풀 기본 + 1:1 매칭 레이어).
 *
 * 최종 점수 = 풀 기본(보상) + 1:1 매칭 부스트 + 공정성 보정 + 신선도.
 *
 * 가중치는 운영 config로 분리(여기선 상수). 실험으로 튜닝 대상.
 */
public class FeedRanker {

    // 가중치 (실험·운영으로 튜닝)
    static final double W_REWARD     = 1.0;    // 풀 기본: 보상 1크레딧당
    static final double BOOST_MATCH  = 10.0;   // 1:1 호혜 부스트
    static final double W_FAIRNESS   = 5.0;    // 공정성: 응답 적을수록
    static final double W_FRESHNESS  = 3.0;    // 신선도

    double score(SurveyCard s, MatchContext ctx) {
        // R1: 풀 기본 — 보상 높을수록
        double base = W_REWARD * s.rewardPerResponse;

        // R2: 1:1 매칭 부스트 — 나에게 응답해준 사람의 설문
        double match = ctx.reciprocalOwners.contains(s.ownerId) ? BOOST_MATCH : 0.0;

        // R3: 공정성 — 누적 응답 적을수록 가산(로그 감쇠로 과도한 역전 방지)
        double fairness = W_FAIRNESS / (1.0 + Math.log1p(s.totalResponsesReceived));

        // R4: 신선도 — 최근 생성일수록 가산(나이를 0~1로 정규화)
        double ageRatio = ctx.nowEpoch > 0
            ? Math.max(0, Math.min(1, (double) s.createdAtEpoch / ctx.nowEpoch))
            : 0;
        double freshness = W_FRESHNESS * ageRatio;

        return base + match + fairness + freshness;
    }

    /**
     * 점수 매기고 정렬 + 다양성 분산 + 상위 K.
     * @param k 반환 개수
     * @param maxPerOwner 같은 제작자 최대 노출 수(R6 다양성)
     */
    List<SurveyCard> rank(List<SurveyCard> cards, MatchContext ctx, int k, int maxPerOwner) {
        // 점수 내림차순 정렬 (R5)
        List<SurveyCard> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Double.compare(score(b, ctx), score(a, ctx)));

        // 다양성 분산 (R6): 같은 제작자 maxPerOwner가 엄격한 상한.
        // 상한 때문에 k를 못 채우면 피드가 짧아지는 걸 허용한다(다양성 우선).
        List<SurveyCard> result = new ArrayList<>();
        Map<String, Integer> ownerCount = new HashMap<>();
        for (SurveyCard s : sorted) {
            int cnt = ownerCount.getOrDefault(s.ownerId, 0);
            if (cnt < maxPerOwner) {
                result.add(s);
                ownerCount.put(s.ownerId, cnt + 1);
            }
            // 초과분은 버린다(보류 후 재투입하면 다양성 제약이 무의미해짐)
            if (result.size() >= k) break;
        }
        return result.size() > k ? result.subList(0, k) : result;   // R7: 상위 K
    }
}
