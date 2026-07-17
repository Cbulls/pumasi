package egovframework.pmsi.auth.service;

import java.util.Map;

/**
 * 인증 서비스.
 * - 데모: userId 선택 로그인 (pmsi.auth.demo-enabled)
 * - 실인증: 이메일 매직링크 요청/검증
 */
public interface AuthService {

    LoginResult login(String userId) throws Exception;

    String resolve(String token) throws Exception;

    void logout(String token) throws Exception;

    /**
     * 매직링크 발급. SMTP 미연동 시 echo 모드에서 token을 응답에 포함.
     * @return { email, expiresAt, echoedToken? }
     */
    Map<String, Object> requestMagicLink(String email, String displayName) throws Exception;

    /** 매직링크 검증 → 세션 발급 */
    LoginResult verifyMagicLink(String token) throws Exception;

    /** 프로필: userId, displayName, email */
    Map<String, String> profile(String userId) throws Exception;
}
