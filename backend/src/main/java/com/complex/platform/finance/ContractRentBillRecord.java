package com.complex.platform.finance;

import java.time.Instant;

/**
 * ② 履约闭环 S2：合同租金逐（合同,期）计费留痕（contract_rent_bills，V162）。
 * 镜像 {@link PropertyFeeBillRecord}：UNIQUE(contractId,period) 幂等，amountCent 来自 rent_terms.baseRentCent，
 * 并联动生成 source_type='contract' 的应收 ar_ap_items（arApItemId）。
 */
public record ContractRentBillRecord(
        String id,
        String contractId,
        String merchantId,
        String projectId,
        String period,
        long amountCent,
        String arApItemId,
        Instant createdAt
) {
}
