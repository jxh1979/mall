package com.complex.platform.settlement;

import java.time.LocalDate;

/** 结算重构批2-3：月度结算单头（settlement_bills 一行）。金额均整数分。 */
public record SettlementBillRecord(
        String id,
        String projectId,
        LocalDate periodStart,
        LocalDate periodEnd,
        long ticketRevenueCent,
        long channelFeeCent,
        long giveawayCostCent,
        long availablePoolCent,
        int retentionBps,
        long retentionCent,
        long spvPoolCent,
        long distributedCent,
        long compServiceFeeCent,
        String distributionMethod,
        String specialApprovalRef,
        String status,
        // 六业态收入归属(terminology-canonical §三):通票收入归属的 L3 管理公司/业态主体(娱乐平台公司)。
        // 由 revenue_stream_attribution 按 (项目, all_pass_ticket) 解析;未配置=null(零金额影响,仅归属留白)。
        String attributedEntityId,
        // D6 套票折扣入账：本月项目公司实际承担的折扣额（platform_absorb 全额/split_by_ratio 平台份额；
        //   merchant_absorb=0）。为覆盖折扣使商户拿满额，项目多支出的金额。默认 0=零行为变更。
        long comboDiscountCent,
        // R-7/D15.1（V343）分账尾差（带符号）：= 应分总额 − Σ各商户已舍入分账额。各方按比例四舍五入到分后
        //   与总额的差额，统一归项目公司/平台兜底并【单列留痕】，不并入任一商户。守恒：Σ分账 + 尾差 ≡ 应分总额。
        long roundingResidualCent
) {
}
