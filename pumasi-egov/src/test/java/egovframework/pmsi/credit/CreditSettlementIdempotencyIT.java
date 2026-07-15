package egovframework.pmsi.credit;

import egovframework.pmsi.credit.service.CreditService;
import egovframework.pmsi.credit.service.SettleCommand;
import egovframework.pmsi.credit.service.SettleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 정산 멱등성 통합 테스트 (실 PostgreSQL 필요).
 *
 * 실행 조건: 환경변수 PUMASI_IT=true + DB 기동. (없으면 자동 skip → 무DB 빌드 통과)
 *   PUMASI_IT=true ./gradlew test --tests '*CreditSettlementIdempotencyIT'
 *
 * 검증: 같은 responseId 로 두 번 정산해도 잔액 변동은 1회만 발생한다
 *       (원장 (reason, ref_id) UNIQUE + 사전 멱등 체크).
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "PUMASI_IT", matches = "true")
class CreditSettlementIdempotencyIT {

    @Autowired
    private CreditService creditService;

    @Test
    void settleIsIdempotent() throws Exception {
        String ownerId = "u-owner";
        String respondentId = "u-alice";
        String formRef = "it-form-" + UUID.randomUUID();
        String responseId = "it-resp-" + UUID.randomUUID();

        // 제작자 예치(available → escrow) 후, cost=5 응답 1건 정산
        creditService.depositEscrow(ownerId, 10, formRef);
        long ownerEscrowBefore = creditService.getBalance(ownerId).getEscrow();
        long respondentBefore = creditService.getBalance(respondentId).getAvailable();

        SettleCommand cmd = new SettleCommand(ownerId, respondentId, 5, responseId);
        SettleResult first = creditService.settle(cmd);
        SettleResult retry = creditService.settle(cmd);   // 중복 정산 시도

        assertFalse(first.alreadyDone(), "첫 정산은 신규여야 한다");
        assertTrue(retry.alreadyDone(), "재정산은 멱등 히트여야 한다");
        assertEquals(4, first.reward());                  // floor(5*0.8)

        long ownerEscrowAfter = creditService.getBalance(ownerId).getEscrow();
        long respondentAfter = creditService.getBalance(respondentId).getAvailable();

        assertEquals(ownerEscrowBefore - 5, ownerEscrowAfter, "escrow는 cost만큼 1회만 차감");
        assertEquals(respondentBefore + first.reward(), respondentAfter, "적립은 reward만큼 1회만");
    }
}
