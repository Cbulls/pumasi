package egovframework.pmsi.config;

import org.egovframe.rte.fdl.cmmn.trace.LeaveaTrace;
import org.egovframe.rte.fdl.cmmn.trace.handler.DefaultTraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.handler.TraceHandler;
import org.egovframe.rte.fdl.cmmn.trace.manager.DefaultTraceHandleManager;
import org.egovframe.rte.fdl.cmmn.trace.manager.TraceHandlerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/**
 * 전자정부표준프레임워크 공통 설정.
 *
 * ★ EgovAbstractServiceImpl 은 @Resource(name="leaveaTrace") 로 LeaveaTrace 빈을 주입받는다.
 *   이 빈이 없으면 컨텍스트 기동 시 NoSuchBeanDefinitionException 이 발생한다.
 *   따라서 표준 개발가이드의 trace 빈 4종(leaveaTrace/traceHandleManager/antPathMatcher/
 *   defaultTraceHandler)을 Java Config 로 등록한다.
 *
 *   LeaveaTrace 내부의 @Resource(name="messageSource") 는 Spring Boot 가 기본 제공하는
 *   messageSource 빈으로 해소된다(별도 정의 불필요).
 */
@Configuration
public class EgovConfigCommon {

    @Bean(name = "leaveaTrace")
    public LeaveaTrace leaveaTrace(DefaultTraceHandleManager traceHandleManager) {
        LeaveaTrace leaveaTrace = new LeaveaTrace();
        leaveaTrace.setTraceHandlerServices(new TraceHandlerService[]{traceHandleManager});
        return leaveaTrace;
    }

    @Bean
    public DefaultTraceHandleManager traceHandleManager(AntPathMatcher antPathMatcher,
                                                        DefaultTraceHandler defaultTraceHandler) {
        DefaultTraceHandleManager manager = new DefaultTraceHandleManager();
        manager.setReqExpMatcher(antPathMatcher);
        manager.setPatterns(new String[]{"*"});
        manager.setHandlers(new TraceHandler[]{defaultTraceHandler});
        return manager;
    }

    @Bean
    public AntPathMatcher antPathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public DefaultTraceHandler defaultTraceHandler() {
        return new DefaultTraceHandler();
    }
}
