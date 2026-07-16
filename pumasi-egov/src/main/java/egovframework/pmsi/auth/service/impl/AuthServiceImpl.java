package egovframework.pmsi.auth.service.impl;

import egovframework.pmsi.auth.service.AuthService;
import egovframework.pmsi.auth.service.LoginResult;
import egovframework.pmsi.cmm.PmsiException;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 인증 서비스 구현체.
 *
 * ★ 표준 규약: EgovAbstractServiceImpl 상속 + @Resource 이름 기반 주입.
 *
 * 데모용: 비밀번호 없이 계정 존재 여부만 확인하고 세션 토큰을 발급한다.
 * 실제 서비스에서는 소셜 로그인(OAuth)/비밀번호 해시로 대체한다.
 */
@Service("authService")
public class AuthServiceImpl extends EgovAbstractServiceImpl implements AuthService {

    private static final long TOKEN_TTL_DAYS = 7;

    @Resource(name = "authDAO")
    private AuthDAO authDAO;

    @Override
    @Transactional
    public LoginResult login(String userId) throws Exception {
        if (userId == null || userId.isBlank() || !authDAO.existsUser(userId)) {
            throw PmsiException.unauthorized("auth.unknown.user", "알 수 없는 계정입니다: " + userId);
        }
        // 세션 행 무한 누적 방지: 이 유저의 만료 세션을 정리하고 새 토큰 발급
        authDAO.deleteExpiredSessions(userId, OffsetDateTime.now());
        String token = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(TOKEN_TTL_DAYS);
        authDAO.insertSession(token, userId, expiresAt);
        return new LoginResult(token, userId, expiresAt.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public String resolve(String token) throws Exception {
        if (token == null || token.isBlank()) return null;
        return authDAO.selectValidUserId(token, OffsetDateTime.now());
    }

    @Override
    @Transactional
    public void logout(String token) throws Exception {
        if (token == null || token.isBlank()) return;
        authDAO.deleteSession(token);
    }
}
