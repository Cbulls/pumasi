package com.pumasiform.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 실제 PG 어댑터 골격 (토스페이먼츠 예시).
 *
 * ⚠️ 이 어댑터는 계약 전에는 완성할 수 없다. 아래 ◆ 표시가 계약 후 채워야 할 자리다.
 *    사업자등록 → PG사 가입·심사 → 계약 → API 키 발급을 거쳐야 동작한다.
 *
 * @Profile("prod") — 운영에서만 활성. 개발/테스트는 FakePaymentGateway가 쓰인다.
 *
 * 중요: 이 클래스를 채우는 동안에도 PaymentService·도메인은 전혀 건드리지 않는다.
 *       포트(PaymentGateway)만 구현하므로, 계약 후 작업은 이 파일에 격리된다.
 */
@Component
@Profile("prod")
public class TossPaymentsGateway implements PaymentGateway {

    // ◆ 계약 후: PG가 발급한 시크릿 키. 환경변수/시크릿 매니저로 주입(코드·git에 두지 않는다).
    @Value("${payment.toss.secret-key:}")
    private String secretKey;

    // ◆ 계약 후 확정: PG 승인 엔드포인트. 토스는 https://api.tosspayments.com/v1/payments/confirm
    @Value("${payment.toss.confirm-url:}")
    private String confirmUrl;

    // HTTP 클라이언트(RestClient/WebClient). 계약과 무관하게 주입 가능.
    // private final RestClient http;

    @Override
    public PaymentApproval approve(ApproveCommand cmd) {
        /*
         * ◆ 계약 후 구현. 토스페이먼츠 승인 API 호출 예시:
         *
         * 요청: POST {confirmUrl}
         *   Authorization: Basic base64(secretKey + ":")
         *   body: { paymentKey, orderId, amount }
         *
         * var res = http.post()
         *     .uri(confirmUrl)
         *     .header("Authorization", "Basic " + base64(secretKey + ":"))
         *     .body(Map.of(
         *         "paymentKey", cmd.paymentKey(),
         *         "orderId", cmd.orderId(),
         *         "amount", cmd.expectedAmount()))   // PG가 이 금액과 결제창 금액을 대조
         *     .retrieve()
         *     .body(TossApproveResponse.class);
         *
         * // PG 응답을 우리 중립 모델로 변환(여기가 어댑터의 핵심 — PG 필드명을 도메인에서 격리)
         * return new PaymentApproval(
         *     res.paymentKey(),          // 또는 res.transactionKey()
         *     res.totalAmount(),         // PG가 실제 승인한 금액
         *     res.method(),
         *     Instant.parse(res.approvedAt()));
         *
         * // 실패 응답(4xx/5xx)은 PG 에러코드를 PaymentException.Code로 매핑:
         * //   잔액부족/한도초과 → DECLINED, 5xx/타임아웃 → GATEWAY_ERROR,
         * //   이미 처리된 결제 → ALREADY_PROCESSED, 금액 불일치 → AMOUNT_MISMATCH
         */
        throw new UnsupportedOperationException(
            "PG 계약 후 구현 필요. 현재는 FakePaymentGateway(@Profile !prod)를 사용한다.");
    }

    @Override
    public PaymentCancellation cancel(String transactionId, String reason) {
        /*
         * ◆ 계약 후: 토스 취소 API
         *   POST https://api.tosspayments.com/v1/payments/{paymentKey}/cancel
         *   body: { cancelReason }
         */
        throw new UnsupportedOperationException("PG 계약 후 구현 필요");
    }

    @Override
    public String providerName() { return "TOSS_PAYMENTS"; }

    /*
     * ◆ 계약 후 추가로 필요한 것들(이 파일 또는 별도 컴포넌트):
     *  1) 웹훅 핸들러: PG가 결제 상태 변경을 비동기 통보(가상계좌 입금 등).
     *     서명 검증(PG 시크릿으로 HMAC) 후 주문 상태 갱신. 멱등 필수(중복 통보 옴).
     *  2) 결제위젯 클라이언트 키(공개 키): 프론트 SDK 초기화용. 시크릿과 별개.
     *  3) 정산/세금계산서: PG 대시보드 또는 정산 API 연동(회계).
     *  4) 환불 정책: 크레딧 일부 사용 후 환불 시 처리(사용분 차감 등 — 도메인 규칙).
     *  5) PCI-DSS: 카드 정보는 절대 우리 서버를 거치지 않게(PG 결제창이 직접 처리).
     */
}
