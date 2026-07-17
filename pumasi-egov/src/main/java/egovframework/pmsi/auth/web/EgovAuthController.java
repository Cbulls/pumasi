package egovframework.pmsi.auth.web;

import egovframework.pmsi.auth.service.AuthService;
import egovframework.pmsi.auth.service.LoginResult;
import egovframework.pmsi.cmm.PmsiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 인증 API.
 *
 *  POST /pmsi/auth/login              데모 계정 선택 로그인
 *  POST /pmsi/auth/magic-link/request 이메일 매직링크 발급
 *  POST /pmsi/auth/magic-link/verify  매직링크 검증 → 세션
 *  GET  /pmsi/auth/me                 세션 확인
 *  GET  /pmsi/auth/profile            프로필
 *  POST /pmsi/auth/logout             로그아웃
 */
@RestController
@RequestMapping("/pmsi/auth")
public class EgovAuthController {

    private static final String BEARER = "Bearer ";

    @Resource(name = "authService")
    private AuthService authService;

    @PostMapping("/login")
    public LoginResult login(@RequestBody Map<String, String> body) throws Exception {
        return authService.login(body.get("userId"));
    }

    @PostMapping("/magic-link/request")
    public Map<String, Object> requestMagicLink(@RequestBody Map<String, String> body) throws Exception {
        return authService.requestMagicLink(body.get("email"), body.get("displayName"));
    }

    @PostMapping("/magic-link/verify")
    public LoginResult verifyMagicLink(@RequestBody Map<String, String> body) throws Exception {
        return authService.verifyMagicLink(body.get("token"));
    }

    @GetMapping("/me")
    public Map<String, String> me(
            @RequestHeader(value = "Authorization", required = false) String auth) throws Exception {
        String userId = authService.resolve(extractToken(auth));
        if (userId == null) {
            throw PmsiException.unauthorized("unauthorized", "세션이 만료되었거나 유효하지 않습니다.");
        }
        return Map.of("userId", userId);
    }

    @GetMapping("/profile")
    public Map<String, String> profile(
            @RequestHeader(value = "Authorization", required = false) String auth) throws Exception {
        String userId = authService.resolve(extractToken(auth));
        if (userId == null) {
            throw PmsiException.unauthorized("unauthorized", "세션이 만료되었거나 유효하지 않습니다.");
        }
        return authService.profile(userId);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String auth) throws Exception {
        authService.logout(extractToken(auth));
        return ResponseEntity.noContent().build();
    }

    private String extractToken(String auth) {
        if (auth == null || !auth.startsWith(BEARER)) return null;
        return auth.substring(BEARER.length()).trim();
    }
}
