package egovframework.pmsi.cmm.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 인증된 사용자 ID를 주입한다.
 *
 * AuthInterceptor가 Bearer 토큰을 검증해 요청 속성에 넣어 둔 값을
 * CurrentUserArgumentResolver가 꺼내 바인딩한다. (X-User-Id 헤더 신뢰 제거)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
