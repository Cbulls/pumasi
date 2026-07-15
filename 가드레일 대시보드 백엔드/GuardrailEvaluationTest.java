import java.util.*;

/**
 * 가드레일 임계 판정 TDD.
 *
 * 핵심 사상(기획):
 *  - 목적은 "예쁜 지표"가 아니라 "망가지기 전에 멈추는 것".
 *  - 모든 지표는 통제군 대비 상대값(절대값은 외부 요인으로 출렁임).
 *  - 명확한 위반은 자동 차단, 오탐 위험 큰 것은 검토 큐(수동).
 *
 * 규칙:
 *  G1. ★상대값 판정★ 실험군 지표를 통제군과 비교(차이가 임계 초과인지).
 *  G2. 불성실 응답률이 통제군 대비 +5%p 초과 → 자동 중단 트리거.
 *  G3. 7일 재방문율이 통제군 대비 -3%p 미만(더 나쁨) → 자동 중단 트리거.
 *  G4. ★유의성 게이트★ 표본이 작으면(최소 표본 미달) 임계 초과여도 중단 안 함
 *      (우연한 변동으로 멀쩡한 실험 중단 방지).
 *  G5. ★자동 vs 수동★ 자전거래 의심은 자동 중단 안 하고 검토 큐로(오탐 위험).
 *  G6. ★쿨다운★ 최근 중단된 군은 쿨다운 동안 재판정 안 함(중단-재개 반복 방지).
 *  G7. 정상 범위면 트리거 없음(NONE).
 */
public class GuardrailEvaluationTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean c) {
        if (c) { pass++; System.out.println("  PASS " + n); }
        else { fail++; System.out.println("  FAIL " + n); }
    }

    static final long NOW = 1_000_000;

    public static void main(String[] args) {
        GuardrailEvaluator ev = new GuardrailEvaluator();

        System.out.println("== G1/G2: 불성실률 상대값 임계 ==");
        {
            // 통제군 불성실 10%, 실험군 16% → +6%p > +5%p 임계, 표본 충분
            var m = metric("arm1").badRate(0.16).controlBadRate(0.10)
                .sample(2000).controlSample(2000).build();
            check("불성실 +6%p → 자동 중단",
                ev.evaluate(m, NOW).action == Action.AUTO_SUSPEND);
            // 통제군 10%, 실험군 13% → +3%p < +5%p
            var ok = metric("arm1").badRate(0.13).controlBadRate(0.10)
                .sample(2000).controlSample(2000).build();
            check("불성실 +3%p → 트리거 없음",
                ev.evaluate(ok, NOW).action == Action.NONE);
        }

        System.out.println("== G3: 재방문율 상대값 임계 ==");
        {
            // 통제군 재방문 40%, 실험군 36% → -4%p < -3%p 임계(더 나쁨)
            var m = metric("arm1").retentionRate(0.36).controlRetentionRate(0.40)
                .sample(2000).controlSample(2000).build();
            check("재방문 -4%p → 자동 중단",
                ev.evaluate(m, NOW).action == Action.AUTO_SUSPEND);
            // -2%p는 임계 안
            var ok = metric("arm1").retentionRate(0.38).controlRetentionRate(0.40)
                .sample(2000).controlSample(2000).build();
            check("재방문 -2%p → 트리거 없음",
                ev.evaluate(ok, NOW).action == Action.NONE);
        }

        System.out.println("== G4: 유의성 게이트(표본 부족) ==");
        {
            // +6%p지만 표본 30 → 우연일 수 있음 → 중단 안 함
            var m = metric("arm1").badRate(0.16).controlBadRate(0.10)
                .sample(30).controlSample(30).build();
            check("표본 부족 → 중단 보류(NONE)",
                ev.evaluate(m, NOW).action == Action.NONE);
        }

        System.out.println("== G5: 자전거래 → 검토 큐(수동) ==");
        {
            var m = metric("arm1").badRate(0.10).controlBadRate(0.10)
                .sample(2000).controlSample(2000)
                .selfDealClusters(3).build();   // 자전거래 클러스터 탐지됨
            var r = ev.evaluate(m, NOW);
            check("자전거래 → 검토 큐(자동 중단 아님)", r.action == Action.MANUAL_REVIEW);
        }

        System.out.println("== G6: 쿨다운 ==");
        {
            var m = metric("arm1").badRate(0.16).controlBadRate(0.10)
                .sample(2000).controlSample(2000)
                .lastSuspendedAt(NOW - 100).build();   // 방금 중단됨
            // 쿨다운 3600초 이내 → 재판정 안 함
            check("쿨다운 중 → 재판정 보류",
                ev.evaluate(m, NOW).action == Action.COOLDOWN);
        }

        System.out.println("== G7: 정상 ==");
        {
            var m = metric("arm1").badRate(0.10).controlBadRate(0.10)
                .retentionRate(0.40).controlRetentionRate(0.40)
                .sample(2000).controlSample(2000).build();
            check("정상 → NONE", ev.evaluate(m, NOW).action == Action.NONE);
        }

        System.out.println("\n결과: " + pass + " pass / " + fail + " fail");
        if (fail > 0) System.exit(1);
    }

    static MetricBuilder metric(String arm) { return new MetricBuilder(arm); }
}
