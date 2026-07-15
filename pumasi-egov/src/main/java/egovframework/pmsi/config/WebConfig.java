package egovframework.pmsi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정.
 *
 * 프론트엔드(Next.js, http://localhost:3000)가 브라우저에서 이 백엔드(:8080)를
 * 교차 오리진으로 호출하므로 허용이 필요하다. 설계서에서 CORS는 미구현(⬜)으로
 * 표기돼 있던 항목을 프론트 연동을 위해 채운다.
 *
 * 운영에서는 allowedOrigins 를 실제 도메인으로 제한할 것.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
