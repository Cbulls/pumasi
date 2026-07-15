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
 *   [비관적 락] 제작자 escrow -cost
 *   [원자 증가] 응답자 available +reward
 *   [원자 증가] 시스템 available +burn
 * 셋 다 성공해야 커밋. 하나라도 실패하면 전부 롤백(화폐 보존).
 *
 * 멱등: (reason=EARN_RESPONSE, ref_id=responseId) UNIQUE. 사전 체크 + DB 제약 2중 방어.
 */
@Service("creditService")
public class CreditServiceImpl extends EgovAbstractServiceImpl implements CreditService {

    /** 원장 사유 코드 */
    private static final String ESCROW_DEPOSIT = "ESCROW_DEPOSIT";
    private static final String SPEND_ESCROW   = "SPEND_ESCROW";
    private static final String EARN_RESPONSE  = "EARN_RESPONSE";
    private static final String BURN           = "BURN";
    private static final String SYSTEM_ACCOUNT = "SYSTEM";

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
        creditDAO.debitEscrow(cmd.ownerId(), s.cost());
        creditDAO.insertLedger(cmd.ownerId(), -s.cost(), SPEND_ESCROW, cmd.responseId());

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
}
