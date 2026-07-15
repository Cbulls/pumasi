package com.pumasiform.payment;

/**
 * 결제 게이트웨이 포트 (의존 역전의 핵심).
 *
 * 결제 도메인은 이 인터페이스에만 의존한다. 토스페이먼츠/포트원/스트라이프 같은
 * 구체적 PG는 이 포트의 구현체(어댑터)로만 존재한다.
 *
 * 왜 이렇게 하나:
 *  - 계약 전: FakePaymentGateway로 전체 결제 흐름을 검증할 수 있다.
 *  - 계약 후: TossPaymentsGateway 등 실제 어댑터만 구현해 끼우면 된다(도메인 무수정).
 *  - PG 교체/멀티 PG: 어댑터만 추가하면 된다.
 *
 * 결제 흐름은 대부분의 국내 PG가 공유하는 2단계 모델을 따른다:
 *   1) 결제창 요청 → 사용자가 PG 결제창에서 인증·결제 (클라이언트)
 *   2) 승인(confirm) → 서버가 PG에 최종 승인 요청 → 크레딧 적립 (서버)
 * "결제됨 != 승인됨"이 핵심. 2단계 승인을 서버가 직접 해야 금액 위변조를 막는다.
 */
public interface PaymentGateway {

    /**
     * 결제 승인. 클라이언트가 결제창에서 받은 paymentKey를 서버가 PG에 보내 최종 확정.
     *
     * 멱등: 같은 orderId로 두 번 호출돼도 PG는 한 번만 승인한다(대부분의 PG가 보장).
     * 금액 검증: PG가 실제 결제된 금액을 반환하므로, 우리가 기대한 금액과 대조해야 한다.
     *
     * @param command 승인에 필요한 정보(orderId, paymentKey, 기대 금액)
     * @return 승인 결과(실제 결제 금액, PG 거래 ID 등)
     * @throws PaymentException 승인 실패(잔액 부족, 카드 거부, 위변조 등)
     */
    PaymentApproval approve(ApproveCommand command);

    /**
     * 결제 취소/환불. 승인된 결제를 되돌린다.
     * @param transactionId PG 거래 ID
     * @param reason 취소 사유
     */
    PaymentCancellation cancel(String transactionId, String reason);

    /** 이 게이트웨이의 식별자(멀티 PG일 때 어떤 PG로 처리됐는지 기록용). */
    String providerName();
}
