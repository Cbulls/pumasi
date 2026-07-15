package egovframework.pmsi.credit.web;

import egovframework.pmsi.credit.service.CreditBalanceVO;
import egovframework.pmsi.credit.service.CreditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 크레딧 잔액 조회 API (검증/데모용).
 *
 * GET /pmsi/credit/{userId} → 가용/예치 잔액
 */
@RestController
@RequestMapping("/pmsi/credit")
public class EgovCreditController {

    @Resource(name = "creditService")
    private CreditService creditService;

    @GetMapping("/{userId}")
    public CreditBalanceVO balance(@PathVariable String userId) throws Exception {
        return creditService.getBalance(userId);
    }
}
