package egovframework.pmsi.form.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 폼 JPA 리포지토리.
 *
 * ★ JPA 유지의 핵심: 표준프레임워크는 JPA/Spring Data JPA에 정해진 규칙을 강제하지 않는다.
 *    (MyBatis만 EgovAbstractMapper 상속 강제) 따라서 우리가 쓰던 JpaRepository를
 *    그대로 쓰되, 위치만 표준 패키지 규약(service.impl)으로 옮긴다.
 *
 * 학습 포인트:
 *  - 만약 MyBatis로 갔다면 이 자리에 FormDAO extends EgovAbstractMapper + 매퍼 XML이
 *    왔을 것이다. JPA를 유지하면 그 보일러플레이트가 통째로 사라진다.
 *  - 빈 이름을 "formJpaRepository"로 명시(ServiceImpl의 @Resource(name=...)와 매칭).
 *    Spring Data JPA는 인터페이스명 기반 빈을 자동 생성하므로 @Repository 불필요.
 */
public interface FormJpaRepository extends JpaRepository<Form, String> {
    List<Form> findByOwnerId(String ownerId);
}
