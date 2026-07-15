package com.pumasiform.payment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * 결제 주문. 크레딧 현금 구매 1건.
 *
 * 상태 기계:
 *   CREATED → APPROVED → CREDITED   (정상: 주문 생성 → PG 승인 → 크레딧 적립)
 *           → FAILED                (승인 실패)
 *   APPROVED → CANCELED             (환불)
 *
 * 핵심 설계:
 *  - 금액은 서버가 결정(상품=크레딧 패키지). 클라이언트는 패키지 ID만 보낸다 → 위변조 차단.
 *  - orderId 멱등: 승인은 orderId당 한 번만 CREDITED로 갈 수 있다(이중 적립 차단).
 *  - 상태 전이 검증으로 중복/역방향 전이 거부.
 */
@Entity
@Table(name = "payment_order")
public class PaymentOrder {

    @Id
    private String orderId = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false)
    private String userId;

    /** 구매하는 크레딧 패키지 ID(금액·크레딧 양은 서버가 이 ID로 결정) */
    @Column(name = "package_id", nullable = false)
    private String packageId;

    /** 서버가 패키지로부터 결정한 결제 금액(원). 클라이언트 입력 아님. */
    @Column(nullable = false)
    private long amount;

    /** 적립될 크레딧 양 */
    @Column(name = "credit_amount", nullable = false)
    private int creditAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(name = "transaction_id")
    private String transactionId;   // PG 거래 ID(승인 후)

    @Column(name = "provider")
    private String provider;        // 처리한 PG 이름

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Version
    private long version;

    protected PaymentOrder() { }
    public PaymentOrder(String userId, String packageId, long amount, int creditAmount) {
        this.userId = userId; this.packageId = packageId;
        this.amount = amount; this.creditAmount = creditAmount;
    }

    /** 승인 완료 표시. CREATED에서만 가능(이중 승인 차단). */
    public void markApproved(String transactionId, String provider, Instant at) {
        if (status != OrderStatus.CREATED)
            throw new PaymentException(PaymentException.Code.ALREADY_PROCESSED,
                "order not in CREATED: " + status);
        this.status = OrderStatus.APPROVED;
        this.transactionId = transactionId;
        this.provider = provider;
        this.approvedAt = at;
    }

    /** 크레딧 적립 완료 표시. APPROVED에서만. */
    public void markCredited() {
        if (status != OrderStatus.APPROVED)
            throw new PaymentException(PaymentException.Code.INVALID_REQUEST,
                "order not in APPROVED: " + status);
        this.status = OrderStatus.CREDITED;
    }

    public void markFailed() {
        if (status == OrderStatus.CREDITED || status == OrderStatus.CANCELED)
            throw new PaymentException(PaymentException.Code.ALREADY_PROCESSED, "terminal");
        this.status = OrderStatus.FAILED;
    }

    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public long getAmount() { return amount; }
    public int getCreditAmount() { return creditAmount; }
    public OrderStatus getStatus() { return status; }
    public String getTransactionId() { return transactionId; }
}

enum OrderStatus { CREATED, APPROVED, CREDITED, FAILED, CANCELED }
