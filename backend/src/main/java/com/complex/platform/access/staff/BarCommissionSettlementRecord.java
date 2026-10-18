package com.complex.platform.access.staff;

import java.time.Instant;
import java.time.LocalDate;

/**
 * CG-07 派驻人员提成月结：按（酒吧商户 → 人员所属主体 → 月）汇总，生成一条内部结算应付（ar_ap_items，direction=payable，
 * source_type=dispatch_commission_transfer）。amountCent＝该月该酒吧派驻人员提成合计（总额收付、不轧差）；dueDate＝月末后的下一个付账日。
 */
public record BarCommissionSettlementRecord(
        String id,
        String month,
        String merchantId,
        String companyId,
        String projectId,
        long amountCent,
        int calculationCount,
        int staffCount,
        String arApItemId,
        LocalDate dueDate,
        String createdBy,
        Instant createdAt
) {
}
