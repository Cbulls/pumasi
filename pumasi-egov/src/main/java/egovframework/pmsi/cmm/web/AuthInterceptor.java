package egovframework.pmsi.cmm.web;

import egovframework.pmsi.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;

/**
 * 인증 인터셉터.
 *
 * Authorization: Bearer <token> 을 검증해 사용자 ID를 요청 속성(currentUserId)에 넣는다.
 * 토큰이 없거나 유효하지 않으면 401. (X-User-Id 헤더 신뢰를 완전히 대체)
 *
 * /pmsi/auth/** 는 WebConfig에서 제외되고, CORS 프리플라이트(OPTIONS)는 통과시킨다.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource(name = "authService")
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // CORS preflight
        }
        // 문항 이미지 GET은 공유 미리보기용으로 인증 생략(POST는 인증 필요)
        String uri = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod())
                && uri != null
                && uri.matches(".*/pmsi/form/[^/]+/media/[^/]+/?$")) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return unauthorized(response, "로그인이 필요합니다.");
        }
        String token = auth.substring("Bearer ".length()).trim();
        String userId = authService.resolve(token);
        if (userId == null) {
            return unauthorized(response, "세션이 만료되었거나 유효하지 않습니다.");
        }
        request.setAttribute(CurrentUserArgumentResolver.ATTR, userId);
        return true;
    }

    private boolean unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"status\":401,\"code\":\"unauthorized\",\"message\":\"" + message + "\"}");
        return false;
    }
}
