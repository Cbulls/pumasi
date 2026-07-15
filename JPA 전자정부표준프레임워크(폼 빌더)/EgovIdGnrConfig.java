package egovframework.pmsi.config;

import egovframework.rte.fdl.idgnr.EgovIdGnrService;
import egovframework.rte.fdl.idgnr.impl.EgovTableIdGnrService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 표준프레임워크 공통 컴포넌트 설정 — 채번(ID Generation) 서비스.
 *
 * ★ 표준프레임워크의 진짜 가치: 채번·페이징·암호화·파일관리 같은 공통 기능을 바닥부터
 *    만들지 않고 실행환경(org.egovframework.rte.*)이 제공하는 걸 쓴다.
 *
 * 현재 품앗이폼은 UUID(엔티티에서 UUID.randomUUID())로 ID를 만들었다. 표준 방식은
 * EgovIdGnrService로 테이블 기반 채번을 한다. 학습용으로 이 차이를 경험한다.
 *
 * 학습 포인트:
 *  - 테이블 채번은 ids 테이블에 다음 번호를 관리. 분산 환경에서도 유일성 보장.
 *  - UUID는 충돌 걱정 없고 분산에 강하지만 길고 가독성이 낮다. 표준 채번은 짧고
 *    순차적이지만 채번 테이블 경합이 있을 수 있다. 트레이드오프를 아는 게 학습 목표.
 *  - 이건 Spring 설정(@Configuration). 표준 4.x는 XML 대신 Java Config도 지원한다.
 */
@Configuration
public class EgovIdGnrConfig {

    /**
     * 폼 ID 채번 서비스. 빈 이름 "egovFormIdGnrService"는 FormServiceImpl의
     * @Resource(name=...)와 매칭된다.
     */
    @Bean(name = "egovFormIdGnrService")
    public EgovIdGnrService egovFormIdGnrService(DataSource dataSource) throws Exception {
        EgovTableIdGnrService service = new EgovTableIdGnrService();
        service.setDataSource(dataSource);
        service.setStrategy("mixStrategy");   // 접두어 + 숫자 전략
        service.setTable("ids");              // 채번 관리 테이블
        service.setTableName("FORM_ID");      // 이 채번기가 쓸 키
        service.afterPropertiesSet();
        return service;
    }
}
