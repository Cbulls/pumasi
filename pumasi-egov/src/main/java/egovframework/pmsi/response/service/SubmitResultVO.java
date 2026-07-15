package egovframework.pmsi.response.service;

/**
 * 응답 제출 결과.
 *
 * @param responseId     저장된 응답 ID
 * @param qualityFlag    pass / hold / reject
 * @param rewardCredited 응답자에게 지급된 크레딧(pass가 아니면 0)
 */
public record SubmitResultVO(String responseId, String qualityFlag, long rewardCredited) {
}
