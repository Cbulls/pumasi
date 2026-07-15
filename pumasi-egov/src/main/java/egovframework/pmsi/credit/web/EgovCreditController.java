package egovframework.pmsi.credit.web;

import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.credit.service.CreditBalanceVO;
import egovframework.pmsi.credit.service.CreditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 크레딧 잔액 조회 API.
 *
 * GET /pmsi/credit/me → 인증된 본인의 가용/예치 잔액
 *
 * 개인정보보호: 타인 잔액을 경로로 조회하던 방식을 제거하고 본인 것만 반환한다.
 */
@RestController
@RequestMapping("/pmsi/credit")
public class EgovCreditController {

    @Resource(name = "creditService")
    private CreditService creditService;

    @GetMapping("/me")
    public CreditBalanceVO myBalance(@CurrentUser String userId) throws Exception {
        return creditService.getBalance(userId);
    }
}
