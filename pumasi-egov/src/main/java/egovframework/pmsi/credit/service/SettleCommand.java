package egovframework.pmsi.credit.service;

/**
 * 응답 1건 확정 정산 명령.
 *
 * @param ownerId      설문 제작자(예치금 차감 대상)
 * @param respondentId 응답자(보상 적립 대상)
 * @param cost         응답 1건당 비용(폼의 cost_credits)
 * @param responseId   멱등 키(중복 정산 차단)
 */
public record SettleCommand(String ownerId, String respondentId, long cost, String responseId) {
}
