package egovframework.pmsi.auth.service.impl;

import egovframework.pmsi.auth.service.AuthService;
import egovframework.pmsi.auth.service.LoginResult;
import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.credit.service.impl.CreditDAO;
import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 인증 서비스 구현체.
 *
 * 데모 로그인은 pmsi.auth.demo-enabled=true 일 때만 허용.
 * 실인증은 이메일 매직링크(요청 → 검증 → Bearer 세션).
 */
@Service("authService")
public class AuthServiceImpl extends EgovAbstractServiceImpl implements AuthService {

    private static final long TOKEN_TTL_DAYS = 7;
    private static final long MAGIC_TTL_MINUTES = 15;
    private static final Pattern EMAIL_RE =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final String SIGNUP_BONUS = "SIGNUP_BONUS";

    @Resource(name = "authDAO")
    private AuthDAO authDAO;

    @Resource(name = "creditDAO")
    private CreditDAO creditDAO;

    @Value("${pmsi.auth.demo-enabled:true}")
    private boolean demoEnabled;

    @Value("${pmsi.auth.magic-link-echo:true}")
    private boolean magicLinkEcho;

    @Value("${pmsi.auth.signup-bonus:50}")
    private long signupBonus;

    @Override
    @Transactional
    public LoginResult login(String userId) throws Exception {
        if (!demoEnabled) {
            throw PmsiException.unauthorized("auth.demo.disabled",
                    "데모 로그인이 비활성화되어 있습니다. 이메일 매직링크로 로그인하세요.");
        }
        if (userId == null || userId.isBlank() || !authDAO.existsUser(userId)) {
            throw PmsiException.unauthorized("auth.unknown.user", "알 수 없는 계정입니다: " + userId);
        }
        return issueSession(userId);
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

    @Override
    @Transactional
    public Map<String, Object> requestMagicLink(String email, String displayName) throws Exception {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            throw PmsiException.badRequest("auth.email.invalid", "유효한 이메일 주소를 입력하세요.");
        }
        String userId = authDAO.selectUserIdByEmail(normalized);
        if (userId == null) {
            userId = "u-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String name = (displayName != null && !displayName.isBlank())
                    ? displayName.trim().substring(0, Math.min(100, displayName.trim().length()))
                    : normalized.split("@")[0];
            authDAO.insertUser(userId, name, normalized);
            grantSignupBonus(userId);
        }
        authDAO.invalidateOpenMagicLinks(normalized);
        String token = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(MAGIC_TTL_MINUTES);
        authDAO.insertMagicLink(token, normalized, userId, expiresAt);

        Map<String, Object> out = new HashMap<>();
        out.put("email", normalized);
        out.put("expiresAt", expiresAt.toString());
        out.put("message", magicLinkEcho
                ? "개발 모드: echoedToken으로 바로 검증할 수 있습니다."
                : "등록된 이메일로 로그인 링크를 보냈습니다(메일 연동 시).");
        if (magicLinkEcho) {
            out.put("echoedToken", token);
        }
        return out;
    }

    @Override
    @Transactional
    public LoginResult verifyMagicLink(String token) throws Exception {
        if (token == null || token.isBlank()) {
            throw PmsiException.unauthorized("auth.magic.invalid", "유효하지 않은 로그인 링크입니다.");
        }
        Map<String, Object> row = authDAO.selectValidMagicLink(token, OffsetDateTime.now());
        if (row == null) {
            throw PmsiException.unauthorized("auth.magic.invalid",
                    "로그인 링크가 만료되었거나 이미 사용되었습니다.");
        }
        String userId = (String) row.get("userId");
        String email = (String) row.get("email");
        if (userId == null || userId.isBlank()) {
            userId = authDAO.selectUserIdByEmail(email);
        }
        if (userId == null) {
            throw PmsiException.unauthorized("auth.magic.invalid", "계정을 찾을 수 없습니다.");
        }
        authDAO.consumeMagicLink(token);
        return issueSession(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> profile(String userId) throws Exception {
        Map<String, Object> row = authDAO.selectUserProfile(userId);
        if (row == null) {
            throw PmsiException.notFound("auth.user.notfound", "사용자를 찾을 수 없습니다.");
        }
        Map<String, String> out = new HashMap<>();
        out.put("userId", String.valueOf(row.get("userId")));
        out.put("displayName", row.get("displayName") != null ? String.valueOf(row.get("displayName")) : "");
        out.put("email", row.get("email") != null ? String.valueOf(row.get("email")) : "");
        return out;
    }

    private LoginResult issueSession(String userId) {
        authDAO.deleteExpiredSessions(userId, OffsetDateTime.now());
        String token = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(TOKEN_TTL_DAYS);
        authDAO.insertSession(token, userId, expiresAt);
        return new LoginResult(token, userId, expiresAt.toString());
    }

    private void grantSignupBonus(String userId) {
        if (signupBonus <= 0) return;
        String refId = "signup:" + userId;
        if (creditDAO.ledgerExists(SIGNUP_BONUS, refId)) return;
        creditDAO.creditAvailable(userId, signupBonus);
        creditDAO.insertLedger(userId, signupBonus, SIGNUP_BONUS, refId);
    }

    private static String normalizeEmail(String email) {
        if (email == null) return null;
        String e = email.trim().toLowerCase(Locale.ROOT);
        if (e.isEmpty() || !EMAIL_RE.matcher(e).matches() || e.length() > 320) return null;
        return e;
    }
}
