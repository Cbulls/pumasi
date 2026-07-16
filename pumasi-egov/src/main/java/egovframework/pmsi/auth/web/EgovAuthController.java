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
 * 인증 API. 이 경로만 인증 인터셉터에서 제외되므로,
 * me/logout은 Authorization 헤더를 직접 파싱한다.
 *
 *  POST /pmsi/auth/login   { "userId": "u-owner" } → { token, userId, expiresAt }
 *  GET  /pmsi/auth/me      토큰 유효성 확인 → { userId } (무효 시 401)
 *  POST /pmsi/auth/logout  세션 무효화 → 204
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

    @GetMapping("/me")
    public Map<String, String> me(
            @RequestHeader(value = "Authorization", required = false) String auth) throws Exception {
        String userId = authService.resolve(extractToken(auth));
        if (userId == null) {
            throw PmsiException.unauthorized("unauthorized", "세션이 만료되었거나 유효하지 않습니다.");
        }
        return Map.of("userId", userId);
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
