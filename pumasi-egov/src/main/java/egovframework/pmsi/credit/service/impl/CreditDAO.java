package egovframework.pmsi.credit.service.impl;

import egovframework.pmsi.credit.service.CreditBalanceVO;
import org.egovframe.rte.psl.dataaccess.EgovAbstractMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

/**
 * 크레딧 DAO — 전자정부표준프레임워크 EgovAbstractMapper 상속(MyBatis).
 *
 * 동시성 설계(D6):
 *  - 제작자 예치금은 핫 계정 → selectForUpdate(비관적 락, SELECT ... FOR UPDATE)
 *  - 응답자 적립은 콜드 계정 → creditAvailable(원자적 증가 UPDATE/UPSERT)
 *  - 원장 (reason, ref_id) UNIQUE 로 멱등(이중 정산 차단)
 *
 * 쿼리는 EgovAbstractMapper가 제공하는 getSqlSession()(MyBatis SqlSession)으로 실행한다
 * (실행환경 버전 간 래퍼 메서드 차이에 안전).
 */
@Repository("creditDAO")
public class CreditDAO extends EgovAbstractMapper {

    private static final String NS = "creditMapper.";

    /** 비관적 락으로 잔액 행을 잠그고 조회(없으면 null) */
    public CreditBalanceVO selectForUpdate(String userId) {
        return getSqlSession().selectOne(NS + "selectForUpdate", userId);
    }

    public CreditBalanceVO selectBalance(String userId) {
        return getSqlSession().selectOne(NS + "selectBalance", userId);
    }

    /** available → escrow 이동(예치) */
    public void moveToEscrow(String userId, long amount) {
        getSqlSession().update(NS + "moveToEscrow", params(userId, amount));
    }

    /** escrow 차감(응답 확정 시 소진) */
    public void debitEscrow(String userId, long amount) {
        getSqlSession().update(NS + "debitEscrow", params(userId, amount));
    }

    /** available 증가(적립). 잔액 행이 없으면 생성(UPSERT). */
    public void creditAvailable(String userId, long amount) {
        getSqlSession().update(NS + "creditAvailable", params(userId, amount));
    }

    /** 멱등 사전 체크: 해당 (reason, refId) 원장이 이미 있는가 */
    public boolean ledgerExists(String reason, String refId) {
        Map<String, Object> p = new HashMap<>();
        p.put("reason", reason);
        p.put("refId", refId);
        Integer cnt = getSqlSession().selectOne(NS + "ledgerExists", p);
        return cnt != null && cnt > 0;
    }

    /** append-only 원장 기록. (reason, ref_id) UNIQUE 위반 시 DuplicateKeyException */
    public void insertLedger(String userId, long delta, String reason, String refId) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("delta", delta);
        p.put("reason", reason);
        p.put("refId", refId);
        getSqlSession().insert(NS + "insertLedger", p);
    }

    private Map<String, Object> params(String userId, long amount) {
        Map<String, Object> p = new HashMap<>();
        p.put("userId", userId);
        p.put("amount", amount);
        return p;
    }
}
