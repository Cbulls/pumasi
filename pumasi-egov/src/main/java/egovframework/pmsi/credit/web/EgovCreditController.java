package egovframework.pmsi.credit.web;

import egovframework.pmsi.cmm.PmsiException;
import egovframework.pmsi.cmm.web.CurrentUser;
import egovframework.pmsi.credit.service.CreditBalanceVO;
import egovframework.pmsi.credit.service.CreditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 크레딧 API.
 *
 * GET  /pmsi/credit/me       → 인증된 본인의 가용/예치 잔액
 * POST /pmsi/credit/purchase → 충전(베타: Fake 결제, pmsi.credit.fake-purchase-enabled)
 *
 * 개인정보보호: 타인 잔액을 경로로 조회하던 방식을 제거하고 본인 것만 반환한다.
 */
@RestController
@RequestMapping("/pmsi/credit")
public class EgovCreditController {

    @Resource(name = "creditService")
    private CreditService creditService;

    @Value("${pmsi.credit.fake-purchase-enabled:true}")
    private boolean fakePurchaseEnabled;

    @GetMapping("/me")
    public CreditBalanceVO myBalance(@CurrentUser String userId) throws Exception {
        return creditService.getBalance(userId);
    }

    /** body: { "amount": 100, "refId": "클라이언트 멱등 키" } */
    @PostMapping("/purchase")
    public CreditBalanceVO purchase(
            @CurrentUser String userId,
            @RequestBody Map<String, Object> body) throws Exception {
        if (!fakePurchaseEnabled) {
            throw PmsiException.badRequest("credit.purchase.disabled",
                    "충전이 비활성화되어 있습니다. 실결제 도입 전까지 관리자에게 문의하세요.");
        }
        long amount = body.get("amount") instanceof Number n ? n.longValue() : 0;
        String refId = body.get("refId") instanceof String s ? s : null;
        return creditService.purchase(userId, amount, refId);
    }
}
