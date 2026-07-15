package egovframework.pmsi.credit.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 정산 금액 계산 순수 로직 테스트(무DB). */
class SettlementCalcTest {

    @Test
    void payout80percent_burn20percent() {
        SettlementCalc.Settlement s = SettlementCalc.compute(5);   // 지급률 0.8
        assertEquals(5, s.cost());
        assertEquals(4, s.reward());   // floor(5*0.8)=4
        assertEquals(1, s.burn());     // 5-4=1
    }

    @Test
    void minCost_guaranteesMinReward_noBurn() {
        SettlementCalc.Settlement s = SettlementCalc.compute(1);
        assertEquals(1, s.reward());   // 최소 지급 보장
        assertEquals(0, s.burn());
    }

    @Test
    void tenCredits() {
        SettlementCalc.Settlement s = SettlementCalc.compute(10);
        assertEquals(8, s.reward());
        assertEquals(2, s.burn());
    }

    @Test
    void zeroCost() {
        SettlementCalc.Settlement s = SettlementCalc.compute(0);
        assertEquals(0, s.reward());
        assertEquals(0, s.burn());
    }

    @Test
    void negativeCostRejected() {
        assertThrows(IllegalArgumentException.class, () -> SettlementCalc.compute(-1));
    }

    @Test
    void settlementPreservesTotal() {
        // 보존: reward + burn == cost (화폐 무결성)
        for (long cost = 0; cost <= 100; cost++) {
            SettlementCalc.Settlement s = SettlementCalc.compute(cost);
            assertEquals(cost, s.reward() + s.burn());
        }
    }
}
