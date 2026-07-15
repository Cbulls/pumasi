package com.pumasiform.payment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 결제 API.
 *
 * POST /api/payments/orders        주문 생성(패키지 ID) → orderId, 금액
 * POST /api/payments/confirm       승인(orderId, paymentKey) → 크레딧 적립
 * GET  /api/payments/packages      구매 가능한 패키지 목록
 *
 * 결제 흐름(클라이언트 관점):
 *  1) GET packages로 패키지 선택
 *  2) POST orders로 주문 생성 → orderId, amount 수신
 *  3) PG 결제창 SDK로 결제(클라이언트) → paymentKey 수신
 *  4) POST confirm으로 서버 승인 → 크레딧 적립 완료
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;
    private final CreditPackageCatalog catalog;

    public PaymentController(PaymentService service, CreditPackageCatalog catalog) {
        this.service = service;
        this.catalog = catalog;
    }

    @GetMapping("/packages")
    public List<CreditPackage> packages() {
        return catalog.all();
    }

    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") String userId) {
        PaymentOrder order = service.createOrder(userId, body.get("packageId"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "orderId", order.getOrderId(),
            "amount", order.getAmount(),
            "creditAmount", order.getCreditAmount()
        ));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@RequestBody Map<String, String> body) {
        PaymentOrder order = service.confirmPayment(body.get("orderId"), body.get("paymentKey"));
        return ResponseEntity.ok(Map.of(
            "orderId", order.getOrderId(),
            "status", order.getStatus().name(),
            "creditAmount", order.getCreditAmount()
        ));
    }

    /** 결제 예외 → 적절한 HTTP 상태. 금액 불일치는 보안 이슈라 명확히 구분. */
    @ExceptionHandler(PaymentException.class)
    ResponseEntity<Map<String, Object>> onPaymentError(PaymentException e) {
        HttpStatus status = switch (e.code) {
            case AMOUNT_MISMATCH -> HttpStatus.BAD_REQUEST;       // 위변조 의심
            case DECLINED -> HttpStatus.PAYMENT_REQUIRED;          // 402
            case GATEWAY_ERROR -> HttpStatus.BAD_GATEWAY;          // 502 재시도 가능
            case ALREADY_PROCESSED -> HttpStatus.CONFLICT;         // 409
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
            .body(Map.of("error", e.code.name(), "message", e.getMessage()));
    }
}
