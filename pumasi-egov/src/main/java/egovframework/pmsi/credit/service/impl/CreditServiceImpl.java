package egovframework.pmsi.credit.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.CreditBalanceVO;
import egovframework.pmsi.credit.service.CreditService;
import egovframework.pmsi.credit.service.SettleCommand;
import egovframework.pmsi.credit.service.SettleResult;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 크레딧 정산 서비스 구현체.
 *
 * ★ 표준 규약: EgovAbstractServiceImpl 상속 + @Resource 이름 기반 주입.
 *
 * 정산은 하나의 트랜잭션에서:
 *   [비관적 락] 제작자 escrow -cost  (available 원장에는 기록하지 않음)
 *   [원자 증가] 응답자 available +reward + EARN_RESPONSE 원장
 *   [원자 증가] 시스템 available +burn + BURN 원장
 *
 * available 불변식:
 *   available = SUM(ledger.delta) WHERE reason IN
 *     (GENESIS, ESCROW_DEPOSIT, ESCROW_REFUND, EARN_RESPONSE, SIGNUP_BONUS, BURN)
 * SPEND_ESCROW는 legacy로만 남을 수 있으며 신규 정산에서는 owner 원장에 쓰지 않는다.
 *
 * 멱등: (reason=EARN_RESPONSE, ref_id=responseId) UNIQUE.
 */
@Service("creditService")
public class CreditServiceImpl extends EgovAbstractServiceImpl implements CreditService {

    /** available에 영향을 주는 원장 사유 */
    private static final String ESCROW_DEPOSIT = "ESCROW_DEPOSIT";
    private static final String ESCROW_REFUND  = "ESCROW_REFUND";
    private static final String EARN_RESPONSE  = "EARN_RESPONSE";
    private static final String BURN           = "BURN";
    private static final String PURCHASE       = "PURCHASE";
    private static final String SYSTEM_ACCOUNT = "SYSTEM";

    /** 1회 충전 상한(베타 Fake 결제 가드) */
    private static final long PURCHASE_MAX = 10_000;

    @Resource(name = "creditDAO")
    private CreditDAO creditDAO;

    @Override
    @Transactional
    public void depositEscrow(String userId, long amount, String refId) throws Exception {
        if (amount <= 0) return;
        // 멱등: 같은 폼(refId)에 대한 예치는 1회만
        if (creditDAO.ledgerExists(ESCROW_DEPOSIT, refId)) {
            return;
        }
        CreditBalanceVO owner = creditDAO.selectForUpdate(userId);   // 비관적 락
        if (owner == null) {
            throw PmsiException.notFound("credit.account.notfound", "잔액 계정 없음: " + userId);
        }
        if (owner.getAvailable() < amount) {
            throw PmsiException.paymentRequired("credit.insufficient",
                    "예치할 크레딧이 부족합니다. 필요=" + amount + ", 보유=" + owner.getAvailable());
        }
        creditDAO.moveToEscrow(userId, amount);
        creditDAO.insertLedger(userId, -amount, ESCROW_DEPOSIT, refId);
    }

    @Override
    @Transactional
    public void refundEscrow(String userId, long amount, String refId) throws Exception {
        if (amount <= 0) return;
        // 멱등: 같은 폼(refId)에 대한 환불은 1회만
        if (creditDAO.ledgerExists(ESCROW_REFUND, refId)) {
            return;
        }
        CreditBalanceVO owner = creditDAO.selectForUpdate(userId);   // 비관적 락
        if (owner == null) {
            throw PmsiException.notFound("credit.account.notfound", "잔액 계정 없음: " + userId);
        }
        if (owner.getEscrow() < amount) {
            throw PmsiException.conflict("credit.escrow.mismatch",
                    "환불할 예치금이 부족합니다. 필요=" + amount + ", escrow=" + owner.getEscrow());
        }
        creditDAO.moveFromEscrow(userId, amount);
        creditDAO.insertLedger(userId, amount, ESCROW_REFUND, refId);
    }

    @Override
    @Transactional
    public SettleResult settle(SettleCommand cmd) throws Exception {
        SettlementCalc.Settlement s = SettlementCalc.compute(cmd.cost());

        // 1차 멱등 체크(이미 정산된 응답이면 변화 없이 반환)
        if (creditDAO.ledgerExists(EARN_RESPONSE, cmd.responseId())) {
            return new SettleResult(s.reward(), s.burn(), true);
        }

        // 1) [비관적 락] 제작자 escrow -cost
        CreditBalanceVO owner = creditDAO.selectForUpdate(cmd.ownerId());
        if (owner == null) {
            throw PmsiException.notFound("credit.account.notfound", "제작자 계정 없음: " + cmd.ownerId());
        }
        if (owner.getEscrow() < s.cost()) {
            throw PmsiException.paymentRequired("credit.escrow.insufficient",
                    "예치금이 소진되어 정산할 수 없습니다. 필요=" + s.cost() + ", escrow=" + owner.getEscrow());
        }
        // escrow만 차감 — available 원장에 SPEND_ESCROW를 쓰지 않음(available 불변식)
        creditDAO.debitEscrow(cmd.ownerId(), s.cost());

        // 2) [원자 증가] 응답자 +reward — 이 원장 INSERT가 멱등의 실질 방어선(UNIQUE)
        creditDAO.creditAvailable(cmd.respondentId(), s.reward());
        creditDAO.insertLedger(cmd.respondentId(), s.reward(), EARN_RESPONSE, cmd.responseId());

        // 3) [원자 증가] 시스템 +burn (burn=0이면 생략)
        if (s.burn() > 0) {
            creditDAO.creditAvailable(SYSTEM_ACCOUNT, s.burn());
            creditDAO.insertLedger(SYSTEM_ACCOUNT, s.burn(), BURN, cmd.responseId());
        }

        return new SettleResult(s.reward(), s.burn(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CreditBalanceVO getBalance(String userId) throws Exception {
        CreditBalanceVO vo = creditDAO.selectBalance(userId);
        if (vo == null) {
            throw PmsiException.notFound("credit.account.notfound", "잔액 계정 없음: " + userId);
        }
        return vo;
    }

    @Override
    @Transactional
    public CreditBalanceVO purchase(String userId, long amount, String refId) throws Exception {
        if (amount <= 0 || amount > PURCHASE_MAX) {
            throw PmsiException.badRequest("credit.purchase.amount",
                    "충전 금액은 1~" + PURCHASE_MAX + " 사이여야 합니다.");
        }
        if (refId == null || refId.isBlank()) {
            throw PmsiException.badRequest("credit.purchase.ref", "멱등 키(refId)가 필요합니다.");
        }
        // 멱등: 같은 refId 충전은 1회만
        if (!creditDAO.ledgerExists(PURCHASE, refId)) {
            creditDAO.creditAvailable(userId, amount);
            creditDAO.insertLedger(userId, amount, PURCHASE, refId);
        }
        return getBalance(userId);
    }
}
