package com.complex.platform.voucher;

import java.util.List;

/** 营销赠发结果（executeWrite 幂等重放载荷）。 */
public record VoucherIssueResultRecord(
        String definitionId,
        String customerId,
        int quantity,
        List<VoucherInstanceRecord> instances
) {
}
