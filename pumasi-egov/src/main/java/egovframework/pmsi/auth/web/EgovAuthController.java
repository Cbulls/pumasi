package egovframework.pmsi.auth.web;

import egovframework.pmsi.auth.service.AuthService;
import egovframework.pmsi.auth.service.LoginResult;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 인증 API. 이 경로만 인증 인터셉터에서 제외된다.
 *
 *  POST /pmsi/auth/login  { "userId": "u-owner" } → { token, userId, expiresAt }
 */
@RestController
@RequestMapping("/pmsi/auth")
public class EgovAuthController {

    @Resource(name = "authService")
    private AuthService authService;

    @PostMapping("/login")
    public LoginResult login(@RequestBody Map<String, String> body) throws Exception {
        return authService.login(body.get("userId"));
    }
}
