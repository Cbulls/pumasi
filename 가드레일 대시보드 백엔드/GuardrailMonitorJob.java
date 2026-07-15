package com.pumasiform.guardrail;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 가드레일 모니터링 잡. 주기적으로 실험군 지표를 평가하고 자동 대응을 집행한다.
 *
 * 흐름(다이어그램과 일치):
 *   집계 지표 로드 → GuardrailEvaluator로 판정 → GuardrailOrchestrator로 집행
 *
 * 검증된 순수 로직(GuardrailEvaluationTest 8 + GuardrailOrchestrationTest 12)을
 * 스케줄 위에서 조율한다. 평가 자체는 사전 정의된 체크포인트에서만(기획: peeking 방지).
 */
@Component
public class GuardrailMonitorJob {

    private final GuardrailMetricRepository metricRepo;
    private final GuardrailEvaluator evaluator;
    private final GuardrailOrchestrator orchestrator;

    public GuardrailMonitorJob(GuardrailMetricRepository metricRepo,
                               GuardrailEvaluator evaluator,
                               GuardrailOrchestrator orchestrator) {
        this.metricRepo = metricRepo;
        this.evaluator = evaluator;
        this.orchestrator = orchestrator;
    }

    /**
     * 10분마다 활성 실험군 평가. 너무 잦으면 다중비교로 거짓양성↑(기획: 체크포인트만).
     * 운영에선 cron으로 정해진 체크포인트(예: 매일 정해진 시각)만 평가하는 것도 방법.
     */
    @Scheduled(fixedDelayString = "PT10M")
    public void evaluateActiveArms() {
        long nowSec = Instant.now().getEpochSecond();
        List<ArmMetric> metrics = metricRepo.loadActiveArmMetrics();
        for (ArmMetric m : metrics) {
            GuardrailResult result = evaluator.evaluate(m, nowSec);
            orchestrator.apply(m.arm, result, nowSec);
        }
    }
}
