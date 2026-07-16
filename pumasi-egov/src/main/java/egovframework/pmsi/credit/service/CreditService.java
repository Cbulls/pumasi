package egovframework.pmsi.credit.service;

/**
 * 크레딧 트랜잭션 서비스 (화폐 무결성).
 *
 * 표준 규약: 인터페이스 + ServiceImpl 분리. throws Exception 관례.
 *
 * 다른 모듈(폼/응답)은 이 포트만 알고 크레딧 내부 구현을 모른다(포트 & 어댑터).
 */
public interface CreditService {

    /** 설문 등록(publish) 예치: available → escrow 로 amount 이동. refId=formId 로 멱등. */
    void depositEscrow(String userId, long amount, String refId) throws Exception;

    /** 설문 마감 환불: escrow → available 로 amount 반환. refId=formId 로 멱등. */
    void refundEscrow(String userId, long amount, String refId) throws Exception;

    /** 응답 1건 확정 정산: 제작자 escrow -cost / 응답자 +reward / 시스템 +burn (원자적, 멱등). */
    SettleResult settle(SettleCommand cmd) throws Exception;

    /** 잔액 조회 */
    CreditBalanceVO getBalance(String userId) throws Exception;
}
