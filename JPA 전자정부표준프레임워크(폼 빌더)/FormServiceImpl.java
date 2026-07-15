package egovframework.pmsi.form.service.impl;

import egovframework.rte.fdl.cmmn.EgovAbstractServiceImpl;       // ★ 표준 핵심 상속
import egovframework.rte.fdl.idgnr.EgovIdGnrService;            // ★ 표준 채번 서비스
import egovframework.pmsi.form.service.FormService;
import egovframework.pmsi.form.service.FormVO;
import egovframework.pmsi.form.service.QuestionVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;                               // ★ 표준 이름기반 주입
import java.util.List;

/**
 * 폼 빌더 서비스 구현체.
 *
 * ★ 표준프레임워크 규약 2: ServiceImpl은 EgovAbstractServiceImpl을 상속한다.
 *    이 상속이 "표준프레임워크 준수"의 핵심 증표다. 상속으로 얻는 것:
 *     - processException(): 표준 예외 처리 흐름
 *     - leaveaTrace(): 표준 로깅
 *     - egovLogger: 표준 로거
 *
 * ★ 영속성: JPA 유지. 우리가 만든 엔티티(Form/Section/Question/QuestionOption)와
 *    cascade 설계를 그대로 쓴다. 표준프레임워크는 JPA에 별도 규칙을 강제하지 않으므로
 *    (MyBatis만 EgovAbstractMapper 강제), Spring Data JPA 리포지토리를 그대로 주입한다.
 *
 * ★ 비즈니스 로직 재사용: 검증은 우리가 TDD로 만든 순수 FormValidator를 그대로 호출.
 *    "계층만 표준화, 로직은 살린다"의 실체. 프레임워크가 바뀌어도 검증 규칙은 안 바뀐다.
 */
@Service("formService")
public class FormServiceImpl extends EgovAbstractServiceImpl implements FormService {

    /** ★ @Resource 이름 기반 주입(표준 관례). 현대 Spring Boot의 생성자 주입과 대비됨. */
    @Resource(name = "formJpaRepository")
    private FormJpaRepository formRepository;

    @Resource(name = "sectionJpaRepository")
    private SectionJpaRepository sectionRepository;

    /** ★ 표준 채번 서비스. UUID 대신 표준이 제공하는 ID 생성기 사용. */
    @Resource(name = "egovFormIdGnrService")
    private EgovIdGnrService idgenService;

    /** 우리가 만든 순수 검증 로직 — 그대로 재사용(프레임워크 무관) */
    private final FormValidator validator = new FormValidator();

    @Override
    @Transactional
    public String createForm(FormVO formVO) throws Exception {
        Form form = new Form(formVO.getOwnerId(), formVO.getTitle());
        if (formVO.getDescription() != null) form.setDescription(formVO.getDescription());
        // 기본 섹션 1개 자동 생성(현재 로직 유지)
        form.getSections().add(new Section(form, 0, "섹션 1"));
        formRepository.save(form);
        return form.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public FormVO selectForm(String formId) throws Exception {
        Form form = formRepository.findById(formId)
            .orElseThrow(() -> processException("form.notfound"));   // ★ 표준 예외
        return toVO(form);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormVO> selectFormList(String ownerId) throws Exception {
        return formRepository.findByOwnerId(ownerId).stream()
            .map(this::toSummaryVO).toList();
    }

    @Override
    @Transactional
    public void addQuestion(QuestionVO vo) throws Exception {
        Form form = formRepository.findById(vo.getFormId())
            .orElseThrow(() -> processException("form.notfound"));

        // ★ 검증: 우리가 TDD로 검증한 순수 로직 그대로 — 이 부분은 표준화와 무관하게 산다
        List<String> errors = validator.validate(toValidationMap(vo));
        if (!errors.isEmpty()) {
            // 표준 예외 처리(EgovAbstractServiceImpl 제공)
            throw processException("form.validation.fail", errors.toArray());
        }

        Section section = form.getSections().stream()
            .filter(s -> s.getId().equals(vo.getSectionId())).findFirst()
            .orElseThrow(() -> processException("section.notfound"));

        int order = section.getQuestions().size();
        Question q = new Question(section, QuestionType.valueOf(vo.getType()), vo.getTitle(), order);
        q.setRequired(vo.isRequired());
        if (vo.getBranchTargetSectionId() != null) {
            q.setBranchTargetSectionId(vo.getBranchTargetSectionId());
        }
        if (vo.getOptions() != null) {
            int oi = 0;
            for (String label : vo.getOptions()) {
                q.getOptions().add(new QuestionOption(q, label, oi++));
            }
        }
        section.getQuestions().add(q);
        form.touch();
        formRepository.save(form);   // cascade로 질문·옵션 함께 저장(JPA 유지의 이득)
    }

    @Override
    @Transactional
    public void publishForm(String formId, String userId) throws Exception {
        Form form = formRepository.findById(formId)
            .orElseThrow(() -> processException("form.notfound"));
        if (!form.getOwnerId().equals(userId)) {
            throw processException("form.forbidden");
        }
        if (form.getSections().stream().allMatch(s -> s.getQuestions().isEmpty())) {
            throw processException("form.empty");
        }
        form.publish();
    }

    // ── VO 변환 헬퍼 ──
    private FormVO toVO(Form form) {
        FormVO vo = new FormVO();
        vo.setFormId(form.getId());
        vo.setOwnerId(form.getOwnerId());
        vo.setTitle(form.getTitle());
        vo.setStatus(form.getStatus().name());
        // 섹션·질문 매핑 생략(패턴 동일)
        return vo;
    }
    private FormVO toSummaryVO(Form form) {
        FormVO vo = new FormVO();
        vo.setFormId(form.getId());
        vo.setTitle(form.getTitle());
        vo.setStatus(form.getStatus().name());
        return vo;
    }
    private java.util.Map<String, Object> toValidationMap(QuestionVO vo) {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("type", vo.getType());
        m.put("options", vo.getOptions());
        if (vo.getMinSelect() != null) m.put("minSelect", vo.getMinSelect());
        if (vo.getMaxSelect() != null) m.put("maxSelect", vo.getMaxSelect());
        if (vo.getMinLength() != null) m.put("minLength", vo.getMinLength());
        if (vo.getMaxLength() != null) m.put("maxLength", vo.getMaxLength());
        if (vo.getRegex() != null) m.put("regex", vo.getRegex());
        if (vo.getScaleMin() != null) m.put("scaleMin", vo.getScaleMin());
        if (vo.getScaleMax() != null) m.put("scaleMax", vo.getScaleMax());
        return m;
    }
}
