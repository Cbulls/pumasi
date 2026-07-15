package egovframework.pmsi.credit.service;

/**
 * 정산 결과.
 *
 * @param reward     응답자에게 지급된 크레딧
 * @param burn       소각된 크레딧(cost - reward)
 * @param alreadyDone 멱등 히트(이미 정산됨)로 이번엔 변화 없음
 */
public record SettleResult(long reward, long burn, boolean alreadyDone) {
}
