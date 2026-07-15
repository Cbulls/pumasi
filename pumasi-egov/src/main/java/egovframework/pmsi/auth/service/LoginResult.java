package egovframework.pmsi.auth.service;

/**
 * 로그인 결과.
 *
 * @param token     이후 요청의 Authorization: Bearer 토큰
 * @param userId    인증된 사용자 식별자
 * @param expiresAt 만료 시각(ISO-8601)
 */
public record LoginResult(String token, String userId, String expiresAt) {
}
