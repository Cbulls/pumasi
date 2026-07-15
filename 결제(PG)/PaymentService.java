package com.pumasiform.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 서비스. 크레딧 현금 구매의 비즈니스 규칙.
 *
 * 결제 3대 위험과 방어:
 *  1) 위변조: 금액을 서버가 결정(패키지 ID로 조회). 승인 후 PG 실제 금액과 재대조.
 *  2) 이중 지급: orderId 멱등 + 상태 기계(CREATED→APPROVED→CREDITED는 1회만).
 *  3) 상태 불일치: 승인과 크레딧 적립을 한 트랜잭션으로. 적립 실패 시 전체 롤백 → PG 취소.
 *
 * PG 비의존: PaymentGateway 포트만 호출. 실제 PG는 어댑터로 주입.
 */
@Service
public class PaymentService {

    private final PaymentGateway gateway;          // 포트 (Fake 또는 실제 PG 어댑터)
    private final PaymentOrderRepository orderRepo;
    private final CreditChargePort creditPort;     // 크레딧 적립 포트(크레딧 모듈)
    private final CreditPackageCatalog catalog;    // 패키지 카탈로그(금액·크레딧 정의)

    public PaymentService(PaymentGateway gateway, PaymentOrderRepository orderRepo,
                          CreditChargePort creditPort, CreditPackageCatalog catalog) {
        this.gateway = gateway;
        this.orderRepo = orderRepo;
        this.creditPort = creditPort;
        this.catalog = catalog;
    }

    /**
     * 1단계: 주문 생성. 클라이언트는 패키지 ID만 보낸다.
     * 금액·크레딧 양은 서버가 카탈로그에서 결정(위변조 차단의 시작).
     * @return orderId — 클라이언트가 이걸로 PG 결제창을 띄운다.
     */
    @Transactional
    public PaymentOrder createOrder(String userId, String packageId) {
        CreditPackage pkg = catalog.find(packageId);   // 없는 패키지면 예외
        PaymentOrder order = new PaymentOrder(userId, packageId, pkg.priceWon(), pkg.credits());
        return orderRepo.save(order);
    }

    /**
     * 2단계: 승인 + 크레딧 적립. 클라이언트가 결제창에서 받은 paymentKey로 서버가 확정.
     *
     * 한 트랜잭션 안에서:
     *   주문 조회 → PG 승인 → 금액 대조 → 상태 APPROVED → 크레딧 적립 → 상태 CREDITED
     * 어느 단계든 실패하면 롤백. PG 승인까지 됐는데 적립 실패면 PG 취소(보상 트랜잭션).
     */
    @Transactional
    public PaymentOrder confirmPayment(String orderId, String paymentKey) {
        PaymentOrder order = orderRepo.findById(orderId)
            .orElseThrow(() -> new PaymentException(
                PaymentException.Code.INVALID_REQUEST, "order not found: " + orderId));

        // 멱등: 이미 적립된 주문이면 그대로 반환(이중 지급 차단)
        if (order.getStatus() == OrderStatus.CREDITED) {
            return order;
        }

        // PG 승인 — 서버가 기대 금액을 함께 보내 PG가 대조하게 한다
        PaymentApproval approval = gateway.approve(
            new ApproveCommand(orderId, paymentKey, order.getAmount()));

        // 위변조 2차 방어: PG가 실제 승인한 금액이 우리 기대와 다르면 즉시 중단
        if (approval.approvedAmount() != order.getAmount()) {
            // 금액 불일치 — 승인됐더라도 크레딧 안 주고 PG 취소
            gateway.cancel(approval.transactionId(), "amount mismatch");
            throw new PaymentException(PaymentException.Code.AMOUNT_MISMATCH,
                "expected " + order.getAmount() + " but approved " + approval.approvedAmount());
        }

        order.markApproved(approval.transactionId(), gateway.providerName(), approval.approvedAt());

        // 크레딧 적립 — orderId를 멱등 키(ref_id)로 (크레딧 원장의 (reason,ref_id) 유니크)
        try {
            creditPort.chargeCredits(order.getUserId(), order.getCreditAmount(), orderId);
            order.markCredited();
        } catch (RuntimeException creditError) {
            // 적립 실패: PG는 승인됐는데 크레딧을 못 줬다 → PG 취소(보상)하고 롤백
            gateway.cancel(approval.transactionId(), "credit charge failed");
            throw new PaymentException(PaymentException.Code.GATEWAY_ERROR,
                "credit charge failed, payment canceled: " + creditError.getMessage());
        }

        return orderRepo.save(order);
    }
}
