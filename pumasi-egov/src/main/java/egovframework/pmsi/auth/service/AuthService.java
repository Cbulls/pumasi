package egovframework.pmsi.auth.service;

/**
 * 인증 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 *
 * 데모용 경량 토큰 방식. 비밀번호·소셜 로그인은 범위 밖(계정 선택 로그인).
 */
public interface AuthService {

    /** 로그인: 계정 검증 후 세션 토큰 발급 */
    LoginResult login(String userId) throws Exception;

    /** 토큰 검증: 유효하면 userId, 아니면 null */
    String resolve(String token) throws Exception;
}
