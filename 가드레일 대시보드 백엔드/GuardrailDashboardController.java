package com.pumasiform.guardrail;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 가드레일 대시보드 API.
 *
 * GET  /api/admin/guardrail/metrics          현재 실험군 지표 + 판정 상태
 * GET  /api/admin/guardrail/review-queue      검토 큐(자전거래 등 수동 판단 대기)
 * POST /api/admin/guardrail/arms/{arm}/resume 중단된 실험군 수동 재개(승인 필요)
 *
 * 대시보드의 목적은 "예쁜 지표"가 아니라 "망가지기 전에 멈추는 것"(기획).
 * 그래서 조회뿐 아니라 수동 재개·검토 액션을 제공한다. 관리자 전용(인가 필요).
 */
@RestController
@RequestMapping("/api/admin/guardrail")
public class GuardrailDashboardController {

    private final GuardrailMetricRepository metricRepo;
    private final GuardrailEvaluator evaluator;
    private final GuardrailOrchestrator orchestrator;

    public GuardrailDashboardController(GuardrailMetricRepository metricRepo,
                                        GuardrailEvaluator evaluator,
                                        GuardrailOrchestrator orchestrator) {
        this.metricRepo = metricRepo;
        this.evaluator = evaluator;
        this.orchestrator = orchestrator;
    }

    /** 실험군별 지표 + 현재 판정(어떤 대응이 걸렸는지). */
    @GetMapping("/metrics")
    public List<Map<String, Object>> metrics() {
        long now = Instant.now().getEpochSecond();
        return metricRepo.loadActiveArmMetrics().stream().map(m -> {
            GuardrailResult r = evaluator.evaluate(m, now);
            return Map.<String, Object>of(
                "arm", m.arm,
                "badRateDiff", m.badRate - m.controlBadRate,
                "retentionDiff", m.retentionRate - m.controlRetentionRate,
                "sample", m.sample,
                "action", r.action.name(),
                "reason", r.reason
            );
        }).toList();
    }

    /** 검토 큐(MANUAL_REVIEW로 쌓인 건). */
    @GetMapping("/review-queue")
    public List<Map<String, Object>> reviewQueue() {
        return metricRepo.loadReviewQueue();
    }

    /**
     * 수동 재개. 자동 중단된 실험군을 사람이 승인해 재개한다(중단 자동, 재개 수동).
     * 승인자 ID는 인증 컨텍스트에서(여기선 헤더로 단순화).
     */
    @PostMapping("/arms/{arm}/resume")
    public ResponseEntity<Map<String, Object>> resume(
            @PathVariable String arm,
            @RequestHeader("X-Admin-Id") String adminId) {
        orchestrator.resume(arm, adminId);   // 승인자 없으면 IllegalArgumentException → 400
        return ResponseEntity.ok(Map.of("arm", arm, "resumedBy", adminId, "status", "RESUMED"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> onBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

/**
 * 가드레일 지표 조회 포트. 측정 인프라(form_metrics_daily, response_session)와
 * 크레딧 원장에서 실험군별 지표를 집계한다. 구현체가 SQL 조인.
 */
interface GuardrailMetricRepository {
    List<ArmMetric> loadActiveArmMetrics();
    List<Map<String, Object>> loadReviewQueue();
}
