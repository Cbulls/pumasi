package egovframework.pmsi.credit.service.impl;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.CreditBalanceVO;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 크레딧 DAO — EgovAbstractMapper 상속(MyBatis).
 *
 * 동시성: 제작자 예치금 SELECT FOR UPDATE(비관적 락).
 * version 컬럼은 변경 카운터일 뿐 CAS(낙관적 락)에는 쓰지 않는다.
 * available 원장 사유: GENESIS / ESCROW_DEPOSIT / ESCROW_REFUND / EARN_RESPONSE / SIGNUP_BONUS / BURN.
 */
@Repository("creditDAO")
public class CreditDAO extends EgovAbstractMapper {

    private static final String NS = "creditMapper.";

    public CreditBalanceVO selectForUpdate(String userId) {
        return getSqlSession().selectOne(NS + "selectForUpdate", userId);
    }

    public CreditBalanceVO selectBalance(String userId) {
        return getSqlSession().selectOne(NS + "selectBalance", userId);
    }

    /** available → escrow. 잔액 부족 시 예외. */
    public void moveToEscrow(String userId, long amount) {
        int n = getSqlSession().update(NS + "moveToEscrow", params(userId, amount));
        if (n == 0) {
            throw PmsiException.paymentRequired("credit.insufficient",
                    "예치할 크레딧이 부족합니다.");
        }
    }

    /** escrow 차감. 부족 시 예외. */
    public void debitEscrow(String userId, long amount) {
        int n = getSqlSession().update(NS + "debitEscrow", params(userId, amount));
        if (n == 0) {
            throw PmsiException.paymentRequired("credit.escrow.insufficient",
                    "예치금이 소진되어 정산할 수 없습니다.");
        }
    }

    /** escrow → available. 부족 시 예외. */
    public void moveFromEscrow(String userId, long amount) {
        int n = getSqlSession().update(NS + "moveFromEscrow", params(userId, amount));
        if (n == 0) {
            throw PmsiException.conflict("credit.escrow.mismatch",
                    "환불할 예치금이 부족합니다.");
        }
    }

    public void creditAvailable(String userId, long amount) {
        getSqlSession().update(NS + "creditAvailable", params(userId, amount));
    }

    public boolean ledgerExists(String reason, String refId) {
        Map<String, Object> p = new HashMap<>();
        p.put("reason", reason);
        p.put("refId", refId);
        Integer cnt = getSqlSession().selectOne(NS + "ledgerExists", p);
        return cnt != null && cnt > 0;
    }

    public void insertLedger(String userId, long delta, String reason, String refId) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("delta", delta);
        p.put("reason", reason);
        p.put("refId", refId);
        getSqlSession().insert(NS + "insertLedger", p);
    }

    public List<Map<String, Object>> selectLedger(String userId, int limit) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("limit", limit);
        return getSqlSession().selectList(NS + "selectLedger", p);
    }

    private Map<String, Object> params(String userId, long amount) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("amount", amount);
        return p;
    }
}
