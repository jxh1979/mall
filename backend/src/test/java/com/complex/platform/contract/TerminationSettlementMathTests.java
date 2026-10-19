package com.complex.platform.contract;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 退租结算纯算法测试：抵扣正/负、零押金、违约金 trigger 语义、负值钳零。无 DB。 */
class TerminationSettlementMathTests {

    private static Map<String, Object> itemOf(TerminationSettlementMath.Result r, String item) {
        return r.items().stream().filter(i -> item.equals(i.get("item"))).findFirst().orElse(null);
    }

    @Test
    void depositFullyRefundedWhenNoUnpaid() {
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("move_out", 500000L, 0L, 0L, null);
        assertThat(r.offsetCent()).isZero();
        assertThat(r.depositRefundCent()).isEqualTo(500000L);
        assertThat(r.merchantStillOwesCent()).isZero();
        Map<String, Object> dep = itemOf(r, "deposit_disposal");
        assertThat(dep).containsEntry("status", "payable_posted").containsEntry("amountCent", 500000L);
    }

    @Test
    void unpaidPartiallyOffsetsDepositNetPositive() {
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("move_out", 500000L, 200000L, 0L, null);
        assertThat(r.offsetCent()).isEqualTo(200000L);
        assertThat(r.depositRefundCent()).isEqualTo(300000L);
        assertThat(r.merchantStillOwesCent()).isZero();
        assertThat(itemOf(r, "deposit_disposal")).containsEntry("direction", "payable");
    }

    @Test
    void unpaidExceedsDepositMerchantStillOwesNoPayable() {
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("move_out", 100000L, 300000L, 0L, null);
        assertThat(r.offsetCent()).isEqualTo(100000L);
        assertThat(r.depositRefundCent()).isZero();
        assertThat(r.merchantStillOwesCent()).isEqualTo(200000L);
        Map<String, Object> dep = itemOf(r, "deposit_disposal");
        assertThat(dep).containsEntry("status", "listed_only").containsEntry("direction", "none");
    }

    @Test
    void zeroDepositProducesNoDepositItem() {
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("move_out", 0L, 100000L, 0L, null);
        assertThat(itemOf(r, "deposit_disposal")).isNull();
        assertThat(r.depositRefundCent()).isZero();
        assertThat(r.merchantStillOwesCent()).isEqualTo(100000L);
    }

    @Test
    void earlyTerminationIncludesPenaltyWithReason() {
        TerminationSettlementMath.Result r =
                TerminationSettlementMath.compute("early_termination", 0L, 0L, 200000L, "提前退租违约");
        assertThat(r.penaltyCent()).isEqualTo(200000L);
        Map<String, Object> pen = itemOf(r, "termination_penalty");
        assertThat(pen).containsEntry("direction", "receivable").containsEntry("reason", "提前退租违约");
    }

    /** BSM L1347「只有清退时押金才能抵扣欠费与违约金」：提前终止违约金并入押金抵扣基数——先抵未清与违约金，余额才退，不足即商户仍欠。 */
    @Test
    void earlyTerminationDepositOffsetsUnpaidAndPenaltyBeforeRefund() {
        TerminationSettlementMath.Result r =
                TerminationSettlementMath.compute("early_termination", 500000L, 300000L, 300000L, "合同条款违约金");
        assertThat(r.offsetCent()).isEqualTo(500000L);
        assertThat(r.depositRefundCent()).isZero();
        assertThat(r.merchantStillOwesCent()).isEqualTo(100000L);
        assertThat(itemOf(r, "deposit_disposal")).containsEntry("status", "listed_only")
                .containsEntry("merchantStillOwesCent", 100000L);
        TerminationSettlementMath.Result rest =
                TerminationSettlementMath.compute("early_termination", 500000L, 100000L, 200000L, null);
        assertThat(rest.offsetCent()).isEqualTo(300000L);
        assertThat(rest.depositRefundCent()).isEqualTo(200000L);
        assertThat(rest.merchantStillOwesCent()).isZero();
        TerminationSettlementMath.Result none =
                TerminationSettlementMath.compute("early_termination", 0L, 0L, 200000L, null);
        assertThat(none.merchantStillOwesCent()).isEqualTo(200000L);
    }

    @Test
    void moveOutIgnoresPenaltyEvenIfPassed() {
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("move_out", 0L, 0L, 200000L, "x");
        assertThat(r.penaltyCent()).isZero();
        assertThat(itemOf(r, "termination_penalty")).isNull();
    }

    @Test
    void negativeInputsClampToZeroAndAlwaysListsUnpaidSummary() {
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("early_termination", -5L, -9L, -3L, null);
        assertThat(r.depositCent()).isZero();
        assertThat(r.unpaidCent()).isZero();
        assertThat(r.penaltyCent()).isZero();
        List<Map<String, Object>> items = r.items();
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).containsEntry("item", "unpaid_receivable_summary").containsEntry("amountCent", 0L);
    }

    @Test
    void depositCappedByLedgerBalanceRefundsBalanceNotContractAmount() {
        // 入参 deposit 已是台账余额封顶值（300000 < 固化 500000）：算法按 300000 退。
        TerminationSettlementMath.Result r = TerminationSettlementMath.compute("move_out", 300000L, 0L, 0L, null);
        assertThat(r.depositRefundCent()).isEqualTo(300000L);
        Map<String, Object> dep = itemOf(r, "deposit_disposal");
        assertThat(dep).containsEntry("depositCent", 300000L).containsEntry("status", "payable_posted");
    }
}
