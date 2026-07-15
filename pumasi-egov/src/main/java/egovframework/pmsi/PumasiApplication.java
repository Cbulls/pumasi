package egovframework.pmsi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 품앗이폼 eGov Walking Skeleton 진입점.
 *
 * 흐름: 폼 생성 → 응답 제출 → 크레딧 정산 → 결과 조회.
 * 아키텍처: 전자정부표준프레임워크(eGovFrame) 계층형 + MyBatis + PostgreSQL.
 *
 * 표준 준수 지점:
 *  - 서비스: 인터페이스 + ServiceImpl(EgovAbstractServiceImpl 상속)
 *  - DAO   : EgovAbstractMapper 상속 + Mapper XML
 *  - DI    : @Resource(name=...) 이름 기반 주입
 */
@SpringBootApplication
public class PumasiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PumasiApplication.class, args);
    }
}
