package egovframework.pmsi.auth.service;

/**
 * 인증 서비스 (표준 규약: 인터페이스 + ServiceImpl).
 *
 * 데모용 경량 토큰 방식. 비밀번호·소셜 로그인은 범위 밖(계정 선택 로그인).
 */
public interface AuthService {

    /** 로그인: 계정 검증 후 세션 토큰 발급(만료 세션 정리 포함) */
    LoginResult login(String userId) throws Exception;

    /** 토큰 검증: 유효하면 userId, 아니면 null */
    String resolve(String token) throws Exception;

    /** 로그아웃: 세션 무효화(존재하지 않는 토큰이면 무시) */
    void logout(String token) throws Exception;
}
