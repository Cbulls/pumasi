package com.pumasiform.payment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 가짜 결제 게이트웨이. PG 계약 전에 전체 결제 흐름을 검증·시연하기 위한 구현.
 *
 * 실제 PG 없이도:
 *  - 주문 생성 → 승인 → 크레딧 적립 흐름을 end-to-end 테스트할 수 있다.
 *  - 금액 불일치, 카드 거부 같은 실패 시나리오를 주입해 방어 로직을 검증할 수 있다.
 *
 * @Profile("!prod") — 운영에서는 비활성. 운영엔 실제 PG 어댑터가 빈으로 등록된다.
 *
 * 이 클래스는 계약과 무관하게 지금 완성할 수 있다. 실제 PG 어댑터로 교체해도
 * PaymentService는 한 줄도 바뀌지 않는다(포트 추상화의 이득).
 */
@Component
@Profile("!prod")
public class FakePaymentGateway implements PaymentGateway {

    // paymentKey → 시뮬레이션할 결과(테스트가 주입). 없으면 정상 승인.
    private final Map<String, Long> approvedAmountByKey = new ConcurrentHashMap<>();
    private final Map<String, PaymentException.Code> failByKey = new ConcurrentHashMap<>();

    /** 테스트 헬퍼: 특정 키에 실패 주입 */
    public void injectFailure(String paymentKey, PaymentException.Code code) {
        failByKey.put(paymentKey, code);
    }
    /** 테스트 헬퍼: 특정 키에 위변조(다른 금액) 주입 */
    public void injectAmount(String paymentKey, long actualAmount) {
        approvedAmountByKey.put(paymentKey, actualAmount);
    }

    @Override
    public PaymentApproval approve(ApproveCommand cmd) {
        // 실패 주입 확인
        PaymentException.Code fail = failByKey.get(cmd.paymentKey());
        if (fail != null) {
            throw new PaymentException(fail, "injected failure: " + fail);
        }
        // 금액: 주입된 게 있으면 그걸로(위변조 시뮬레이션), 없으면 기대 금액대로 정상 승인
        long approvedAmount = approvedAmountByKey.getOrDefault(cmd.paymentKey(), cmd.expectedAmount());
        return new PaymentApproval(
            "fake_txn_" + UUID.randomUUID(),
            approvedAmount,
            "CARD",
            Instant.now()
        );
    }

    @Override
    public PaymentCancellation cancel(String transactionId, String reason) {
        return new PaymentCancellation(transactionId, 0, Instant.now());
    }

    @Override
    public String providerName() { return "FAKE"; }
}
