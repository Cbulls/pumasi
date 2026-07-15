import java.util.*;
import java.util.stream.Collectors;

/** 피드 후보 설문 (추천 입력) */
class SurveyCard {
    String id, ownerId, status = "ACTIVE";
    int escrow = 1000, rewardPerResponse = 4;
    int maxResponses = Integer.MAX_VALUE, currentResponses = 0;
    boolean deadlinePassed = false;
    String targetAge = null;                 // null = 전체 대상
    // 점수 매기기용 (STEP 2)
    long createdAtEpoch = 0;
    int totalResponsesReceived = 0;          // 누적 응답 수(공정성용)
}

/** 빌더 (테스트 가독성) */
class SurveyBuilder {
    private final SurveyCard s = new SurveyCard();
    SurveyBuilder(String id) { s.id = id; }
    SurveyBuilder owner(String o) { s.ownerId = o; return this; }
    SurveyBuilder status(String st) { s.status = st; return this; }
    SurveyBuilder escrow(int e) { s.escrow = e; return this; }
    SurveyBuilder rewardPerResponse(int r) { s.rewardPerResponse = r; return this; }
    SurveyBuilder reward(int r) { s.rewardPerResponse = r; return this; }   // 짧은 별칭
    SurveyBuilder maxResponses(int m) { s.maxResponses = m; return this; }
    SurveyBuilder currentResponses(int c) { s.currentResponses = c; return this; }
    SurveyBuilder deadlinePassed(boolean d) { s.deadlinePassed = d; return this; }
    SurveyBuilder targetAge(String a) { s.targetAge = a; return this; }
    SurveyBuilder createdAt(long e) { s.createdAtEpoch = e; return this; }
    SurveyBuilder totalReceived(int t) { s.totalResponsesReceived = t; return this; }
    SurveyCard build() { return s; }
}

/** 응답자(피드를 보는 사람) */
class Viewer {
    String userId, ageGroup;
    Set<String> respondedFormIds;
    Viewer(String userId, String ageGroup, Set<String> responded) {
        this.userId = userId; this.ageGroup = ageGroup; this.respondedFormIds = responded;
    }
}

/**
 * 피드 자격 필터. 응답자에게 "보여줄 수 있는" 설문만 거른다.
 * 점수 매기기 전 단계 — 자격 없는 설문은 점수 계산조차 하지 않는다.
 */
public class FeedEligibility {

    boolean isEligible(SurveyCard s, Viewer viewer) {
        if (s.ownerId.equals(viewer.userId)) return false;               // E1: 자기 설문
        if (viewer.respondedFormIds.contains(s.id)) return false;        // E2: 이미 응답
        if (!"ACTIVE".equals(s.status)) return false;                    // E3: 비활성
        if (s.escrow < s.rewardPerResponse) return false;                // E4: 예치금 소진
        if (s.currentResponses >= s.maxResponses) return false;          // E5: 목표 달성
        if (s.deadlinePassed) return false;                              // E5: 마감
        if (s.targetAge != null && !s.targetAge.equals(viewer.ageGroup)) // E6: 타깃 불일치
            return false;
        return true;                                                     // E7
    }

    List<SurveyCard> filterEligible(List<SurveyCard> all, Viewer viewer) {
        return all.stream().filter(s -> isEligible(s, viewer)).collect(Collectors.toList());
    }
}
