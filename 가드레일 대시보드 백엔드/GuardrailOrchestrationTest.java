import java.util.*;

/**
 * 가드레일 자동 대응 오케스트레이션 TDD.
 *
 * 판정(GuardrailEvaluator)이 AUTO_SUSPEND를 내면 실제로 집행하는 부분.
 *
 * 규칙:
 *  O1. AUTO_SUSPEND → 해당 arm 트래픽 0 + 기본 계수 복귀(feature flag) + 알림 발행.
 *  O2. ★중단은 자동, 재개는 수동★ resume()은 사람 승인(approverId) 필수. 자동 재개 없음.
 *  O3. MANUAL_REVIEW → 검토 큐 적재 + 알림(중단은 안 함).
 *  O4. ★알림 멱등★ 같은 arm 같은 사유로 이미 알렸으면 중복 알림 안 함(알림 폭풍 방지).
 *  O5. NONE/COOLDOWN → 아무 동작 없음.
 *  O6. 중단 집행 시 중단 시각 기록(쿨다운 계산 근거).
 */
public class GuardrailOrchestrationTest {
    static int pass=0, fail=0;
    static void check(String n, boolean c){ if(c){pass++;System.out.println("  PASS "+n);}else{fail++;System.out.println("  FAIL "+n);} }

    public static void main(String[] a){
        System.out.println("== O1: 자동 중단 집행 ==");
        {
            FakeFlags flags=new FakeFlags(); FakeAlerts alerts=new FakeAlerts(); FakeReview review=new FakeReview();
            var orch=new GuardrailOrchestrator(flags, alerts, review);
            orch.apply("arm1", new GuardrailResult(Action.AUTO_SUSPEND, "bad rate +6%p"), 1000);
            check("arm 트래픽 0", flags.suspended.contains("arm1"));
            check("알림 발행", alerts.sent.size()==1);
            check("중단 시각 기록", flags.suspendedAt.get("arm1")==1000L);
        }

        System.out.println("== O2: 재개는 수동 승인 ==");
        {
            FakeFlags flags=new FakeFlags(); FakeAlerts alerts=new FakeAlerts(); FakeReview review=new FakeReview();
            var orch=new GuardrailOrchestrator(flags, alerts, review);
            orch.apply("arm1", new GuardrailResult(Action.AUTO_SUSPEND, "x"), 1000);
            // 승인 없이 재개 시도 → 거부
            boolean rejected=false;
            try { orch.resume("arm1", null); } catch(IllegalArgumentException e){ rejected=true; }
            check("승인자 없이 재개 → 거부", rejected);
            check("여전히 중단 상태", flags.suspended.contains("arm1"));
            // 승인자와 함께 재개 → 성공
            orch.resume("arm1", "admin-kim");
            check("승인자와 재개 → 성공", !flags.suspended.contains("arm1"));
        }

        System.out.println("== O3: 검토 큐 ==");
        {
            FakeFlags flags=new FakeFlags(); FakeAlerts alerts=new FakeAlerts(); FakeReview review=new FakeReview();
            var orch=new GuardrailOrchestrator(flags, alerts, review);
            orch.apply("arm1", new GuardrailResult(Action.MANUAL_REVIEW, "self-deal"), 1000);
            check("검토 큐 적재", review.queued.size()==1);
            check("중단은 안 함", !flags.suspended.contains("arm1"));
            check("알림은 발행", alerts.sent.size()==1);
        }

        System.out.println("== O4: 알림 멱등 ==");
        {
            FakeFlags flags=new FakeFlags(); FakeAlerts alerts=new FakeAlerts(); FakeReview review=new FakeReview();
            var orch=new GuardrailOrchestrator(flags, alerts, review);
            var r=new GuardrailResult(Action.AUTO_SUSPEND, "bad rate +6%p");
            orch.apply("arm1", r, 1000);
            orch.apply("arm1", r, 1100);   // 같은 arm 같은 사유 재평가
            check("중복 알림 안 함(1건)", alerts.sent.size()==1);
        }

        System.out.println("== O5: NONE/COOLDOWN 무동작 ==");
        {
            FakeFlags flags=new FakeFlags(); FakeAlerts alerts=new FakeAlerts(); FakeReview review=new FakeReview();
            var orch=new GuardrailOrchestrator(flags, alerts, review);
            orch.apply("arm1", new GuardrailResult(Action.NONE, "ok"), 1000);
            orch.apply("arm2", new GuardrailResult(Action.COOLDOWN, "cd"), 1000);
            check("아무 중단 없음", flags.suspended.isEmpty());
            check("아무 알림 없음", alerts.sent.isEmpty());
        }

        System.out.println("\n결과: "+pass+" pass / "+fail+" fail");
        if(fail>0) System.exit(1);
    }
}

class FakeFlags implements FeatureFlagPort {
    Set<String> suspended=new HashSet<>(); Map<String,Long> suspendedAt=new HashMap<>();
    public void suspendArm(String arm, long at){ suspended.add(arm); suspendedAt.put(arm, at); }
    public void resumeArm(String arm){ suspended.remove(arm); }
}
class FakeAlerts implements AlertPort {
    List<String> sent=new ArrayList<>();
    public void publish(String arm, String reason){ sent.add(arm+":"+reason); }
}
class FakeReview implements ReviewQueuePort {
    List<String> queued=new ArrayList<>();
    public void enqueue(String arm, String reason){ queued.add(arm); }
}
