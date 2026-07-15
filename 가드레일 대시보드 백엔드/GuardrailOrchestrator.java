import java.util.*;

/* ── 포트(의존 역전): 오케스트레이터가 인프라에 직접 의존하지 않게 ── */

/** feature flag 포트. arm 트래픽 제어(중단 시 0 + 기본 계수 복귀). */
interface FeatureFlagPort {
    void suspendArm(String arm, long at);
    void resumeArm(String arm);
}

/** 알림 포트. 임계 초과 이벤트를 메시지 큐 → Slack/이메일/PagerDuty. */
interface AlertPort {
    void publish(String arm, String reason);
}

/** 검토 큐 포트. 오탐 위험 큰 건(자전거래) 사람 검토. */
interface ReviewQueuePort {
    void enqueue(String arm, String reason);
}

/**
 * 가드레일 자동 대응 오케스트레이터.
 *
 * 핵심 원칙(기획):
 *  - 중단은 자동, 재개는 수동. resume()은 승인자 필수 — 오탐으로 멈춘 실험이
 *    자동 재개되며 중단-재개를 반복하지 않게.
 *  - 알림 멱등: 같은 arm 같은 사유는 한 번만 알림(알림 폭풍 방지).
 */
public class GuardrailOrchestrator {

    private final FeatureFlagPort flags;
    private final AlertPort alerts;
    private final ReviewQueuePort review;
    private final Set<String> alertedKeys = new HashSet<>();   // 멱등용 (arm:reason)

    public GuardrailOrchestrator(FeatureFlagPort flags, AlertPort alerts, ReviewQueuePort review) {
        this.flags = flags;
        this.alerts = alerts;
        this.review = review;
    }

    /** 판정 결과를 집행. */
    public void apply(String arm, GuardrailResult result, long nowSec) {
        switch (result.action) {
            case AUTO_SUSPEND -> {
                flags.suspendArm(arm, nowSec);          // O1: 트래픽 0 + 기본 계수 복귀
                alertOnce(arm, result.reason);          // O4: 멱등 알림
            }
            case MANUAL_REVIEW -> {
                review.enqueue(arm, result.reason);     // O3: 검토 큐(중단 안 함)
                alertOnce(arm, result.reason);
            }
            case NONE, COOLDOWN -> { /* O5: 무동작 */ }
        }
    }

    /**
     * 실험군 재개. O2: 반드시 승인자가 있어야 한다(수동 재개).
     * @throws IllegalArgumentException 승인자 없이 재개 시도
     */
    public void resume(String arm, String approverId) {
        if (approverId == null || approverId.isBlank()) {
            throw new IllegalArgumentException("resume requires manual approver");
        }
        flags.resumeArm(arm);
        // 재개되면 알림 멱등 키 해제(다음 위반 시 다시 알릴 수 있게)
        alertedKeys.removeIf(k -> k.startsWith(arm + ":"));
    }

    /** 같은 arm 같은 사유는 한 번만 알림. */
    private void alertOnce(String arm, String reason) {
        String key = arm + ":" + reason;
        if (alertedKeys.add(key)) {   // add가 true면 처음 → 알림
            alerts.publish(arm, reason);
        }
    }
}
