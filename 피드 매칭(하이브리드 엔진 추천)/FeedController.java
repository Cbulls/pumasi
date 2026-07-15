package com.pumasiform.feed;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 피드 API. 응답자에게 "지금 응답할 설문"을 하이브리드로 추천.
 *
 * GET /api/feed?limit=20   → 자격 필터 → 하이브리드 점수 → 다양성 분산 → 상위 K
 *
 * 하이브리드(D1): 풀 기본(보상) + 1:1 매칭 부스트(호혜) + 공정성 + 신선도.
 */
@RestController
@RequestMapping("/api/feed")
class FeedController {

    private final FeedService service;
    FeedController(FeedService service) { this.service = service; }

    @GetMapping
    List<FeedItem> feed(@RequestParam(defaultValue = "20") int limit,
                        @RequestHeader("X-User-Id") String userId) {
        return service.buildFeed(userId, limit);
    }
}

record FeedItem(String formId, String title, int rewardPerResponse, double score) { }

/**
 * 피드 서비스. 검증된 순수 로직(FeedEligibility, FeedRanker)을 DB 위에서 조율.
 *
 * 데이터 소스:
 *  - 후보 설문: 폼 빌더(ACTIVE) + 크레딧(escrow) + 응답 수집(currentResponses)
 *  - viewer 컨텍스트: 응답 이력(이미 응답한 폼), 프로필(연령대)
 *  - 매칭 컨텍스트: "나에게 응답해준 사람"(응답 수집에서 역방향 조회)
 *
 * 성능: 후보를 DB에서 1차로 좁힌다(WHERE status='ACTIVE' AND escrow>=reward
 *       AND NOT 마감). 자격 필터의 자기/응답/타깃은 애플리케이션에서 정밀 적용.
 */
@Service
class FeedService {

    private final FeedQueryPort query;
    private final FeedEligibility eligibility = new FeedEligibility();
    private final FeedRanker ranker = new FeedRanker();

    static final int MAX_PER_OWNER = 3;   // 다양성: 한 제작자 최대 노출

    FeedService(FeedQueryPort query) { this.query = query; }

    @Transactional(readOnly = true)
    List<FeedItem> buildFeed(String userId, int limit) {
        // 1) viewer 컨텍스트
        Viewer viewer = query.loadViewer(userId);

        // 2) 후보 설문(DB 1차 필터된 ACTIVE 풀)
        List<SurveyCard> candidates = query.loadActiveCandidates();

        // 3) 자격 정밀 필터(자기/응답/타깃/소진)
        List<SurveyCard> eligible = eligibility.filterEligible(candidates, viewer);

        // 4) 매칭 컨텍스트("나에게 응답해준 사람")
        MatchContext ctx = new MatchContext(userId,
            query.loadReciprocalOwners(userId), System.currentTimeMillis() / 1000);

        // 5) 하이브리드 점수 + 다양성 + 상위 K
        List<SurveyCard> ranked = ranker.rank(eligible, ctx, limit, MAX_PER_OWNER);

        return ranked.stream()
            .map(s -> new FeedItem(s.id, query.titleOf(s.id), s.rewardPerResponse,
                ranker.score(s, ctx)))
            .toList();
    }
}

/**
 * 피드 조회 포트(의존 역전). 피드 모듈이 폼·응답·크레딧 모듈 내부에 직접 의존하지 않게.
 * 구현체가 여러 테이블을 조인해 후보·컨텍스트를 만든다.
 */
interface FeedQueryPort {
    Viewer loadViewer(String userId);
    List<SurveyCard> loadActiveCandidates();
    java.util.Set<String> loadReciprocalOwners(String userId);
    String titleOf(String formId);
}
