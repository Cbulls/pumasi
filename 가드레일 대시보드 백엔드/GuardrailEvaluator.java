import java.util.*;

/** 자동 대응 종류 */
enum Action {
    NONE,            // 정상
    AUTO_SUSPEND,    // 자동 중단(명확한 위반 + 유의)
    MANUAL_REVIEW,   // 검토 큐(오탐 위험 — 자전거래 등)
    COOLDOWN         // 쿨다운 중(재판정 보류)
}

/** 평가 결과 */
class GuardrailResult {
    Action action;
    String reason;
    GuardrailResult(Action action, String reason) { this.action = action; this.reason = reason; }
}

/** 실험군 지표 스냅샷 (통제군 대비값 포함) */
class ArmMetric {
    String arm;
    double badRate, controlBadRate;            // 불성실 응답률
    double retentionRate = 0.4, controlRetentionRate = 0.4;   // 7일 재방문율
    int sample, controlSample;
    int selfDealClusters = 0;                  // 자전거래 의심 클러스터 수
    Long lastSuspendedAt = null;               // 마지막 자동 중단 시각(쿨다운용)
}

class MetricBuilder {
    private final ArmMetric m = new ArmMetric();
    MetricBuilder(String arm) { m.arm = arm; }
    MetricBuilder badRate(double r) { m.badRate = r; return this; }
    MetricBuilder controlBadRate(double r) { m.controlBadRate = r; return this; }
    MetricBuilder retentionRate(double r) { m.retentionRate = r; return this; }
    MetricBuilder controlRetentionRate(double r) { m.controlRetentionRate = r; return this; }
    MetricBuilder sample(int n) { m.sample = n; return this; }
    MetricBuilder controlSample(int n) { m.controlSample = n; return this; }
    MetricBuilder selfDealClusters(int n) { m.selfDealClusters = n; return this; }
    MetricBuilder lastSuspendedAt(long t) { m.lastSuspendedAt = t; return this; }
    ArmMetric build() { return m; }
}

/**
 * 가드레일 평가기. 지표 스냅샷 → 자동 대응 판정.
 *
 * 판정 순서(우선순위):
 *   1) 쿨다운 중이면 보류(중단-재개 반복 방지).
 *   2) 자전거래 의심이면 검토 큐(자동 중단 안 함 — 오탐 위험).
 *   3) 불성실률/재방문율이 임계 초과 + 통계적 유의 → 자동 중단.
 *   4) 아니면 NONE.
 *
 * 모든 임계는 통제군 대비 상대값. 유의성은 두 비율 z-검정으로 우연 변동 배제.
 */
public class GuardrailEvaluator {

    static final double BAD_RATE_THRESHOLD = 0.05;       // +5%p
    static final double RETENTION_THRESHOLD = -0.03;     // -3%p
    static final int MIN_SAMPLE = 100;                   // 유의성 판정 최소 표본
    static final double Z_CRITICAL = 1.96;               // 양측 95%
    static final long COOLDOWN_SEC = 3600;               // 1시간

    GuardrailResult evaluate(ArmMetric m, long nowSec) {
        // 1) 쿨다운
        if (m.lastSuspendedAt != null && nowSec - m.lastSuspendedAt < COOLDOWN_SEC) {
            return new GuardrailResult(Action.COOLDOWN, "in cooldown window");
        }

        // 2) 자전거래 → 검토 큐(자동 중단 안 함, 오탐 위험)
        if (m.selfDealClusters > 0) {
            return new GuardrailResult(Action.MANUAL_REVIEW,
                "self-deal clusters: " + m.selfDealClusters);
        }

        // 3) 불성실률 임계 + 유의성
        double badDiff = m.badRate - m.controlBadRate;
        if (badDiff > BAD_RATE_THRESHOLD
                && isSignificant(m.badRate, m.controlBadRate, m.sample, m.controlSample)) {
            return new GuardrailResult(Action.AUTO_SUSPEND,
                String.format("bad rate +%.1f%%p (significant)", badDiff * 100));
        }

        // 3) 재방문율 임계 + 유의성 (음수가 더 나쁨)
        double retDiff = m.retentionRate - m.controlRetentionRate;
        if (retDiff < RETENTION_THRESHOLD
                && isSignificant(m.retentionRate, m.controlRetentionRate, m.sample, m.controlSample)) {
            return new GuardrailResult(Action.AUTO_SUSPEND,
                String.format("retention %.1f%%p (significant)", retDiff * 100));
        }

        return new GuardrailResult(Action.NONE, "within bounds");
    }

    /**
     * 두 비율 차이의 통계적 유의성 (z-검정).
     * 표본이 작으면 우연한 변동이므로 유의하지 않다고 본다(G4 — 멀쩡한 실험 보호).
     */
    private boolean isSignificant(double p1, double p2, int n1, int n2) {
        if (n1 < MIN_SAMPLE || n2 < MIN_SAMPLE) return false;   // 표본 부족 → 보류
        // 합동 비율(pooled proportion)
        double pPool = (p1 * n1 + p2 * n2) / (n1 + n2);
        double se = Math.sqrt(pPool * (1 - pPool) * (1.0 / n1 + 1.0 / n2));
        if (se == 0) return false;
        double z = Math.abs(p1 - p2) / se;
        return z > Z_CRITICAL;
    }
}
