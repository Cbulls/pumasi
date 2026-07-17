package egovframework.pmsi.auth.service.impl;

import egovframework.pmsi.auth.service.AuthService;
import egovframework.pmsi.auth.service.LoginResult;
import egovframework.pmsi.cmm.PmsiException;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.beans.factory.annotation.Value;
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
 * 데모용 패스워드리스 로그인은 pmsi.auth.demo-enabled=true 일 때만 허용한다.
 * 프로덕션(false)에서는 OAuth/비밀번호 인증으로 대체하기 전까지 로그인 자체를 차단한다.
 */
@Service("authService")
public class AuthServiceImpl extends EgovAbstractServiceImpl implements AuthService {

    private static final long TOKEN_TTL_DAYS = 7;

    @Resource(name = "authDAO")
    private AuthDAO authDAO;

    @Value("${pmsi.auth.demo-enabled:true}")
    private boolean demoEnabled;

    @Override
    @Transactional
    public LoginResult login(String userId) throws Exception {
        if (!demoEnabled) {
            throw PmsiException.unauthorized("auth.demo.disabled",
                    "데모 로그인이 비활성화되어 있습니다. 정식 인증을 사용하세요.");
        }
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
