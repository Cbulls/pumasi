package egovframework.pmsi.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis + 전자정부표준프레임워크 데이터 접근 설정.
 *
 * ★ EgovAbstractMapper(psl.dataaccess)는 이름이 "sqlSession"인 SqlSessionFactory 빈을
 *   @Resource(name="sqlSession") setSqlSessionFactory(...) 로 주입받는다.
 *   그래서 아래 SqlSessionFactory 빈 이름을 반드시 "sqlSession"으로 맞춘다.
 *
 * ★ mybatis-spring-boot-starter의 자동설정을 쓰지 않고 표준 방식대로 직접 빈을 구성한다
 *   (eGov 개발가이드 context-mapper.xml 의 sqlSession 빈에 대응).
 */
@Configuration
public class EgovConfigAppMapper {

    /**
     * ★ 빈 이름 "sqlSession" (타입 SqlSessionFactory).
     * 매퍼 XML 위치와 전역 설정을 지정한다.
     */
    @Bean(name = "sqlSession")
    public SqlSessionFactory sqlSession(DataSource dataSource, ApplicationContext ctx)
            throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfigLocation(
                ctx.getResource("classpath:/egovframework/mapper/config/mapper-config.xml"));
        bean.setMapperLocations(
                ctx.getResources("classpath:/egovframework/mapper/pmsi/**/*.xml"));
        return bean.getObject();
    }
}
