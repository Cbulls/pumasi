package egovframework.pmsi.config;

import egovframework.pmsi.cmm.web.AuthInterceptor;
import egovframework.pmsi.cmm.web.CurrentUserArgumentResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 웹 공통 설정: CORS + 인증 인터셉터 + @CurrentUser 리졸버.
 *
 * CORS: 허용 오리진은 pmsi.cors.allowed-origins(콤마 구분)로 주입 — 배포 환경별 설정.
 * 인증: /pmsi/** 는 Bearer 토큰 필요(/pmsi/auth/** 제외). X-User-Id 신뢰 제거.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Value("${pmsi.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public WebConfig(AuthInterceptor authInterceptor,
                     CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.authInterceptor = authInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition")
                .allowCredentials(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/pmsi/**")
                .excludePathPatterns("/pmsi/auth/**", "/pmsi/public/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
