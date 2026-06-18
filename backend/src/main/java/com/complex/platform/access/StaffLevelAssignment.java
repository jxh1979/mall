package com.complex.platform.access;

import java.time.Instant;

/**
 * 员工职级与具体薪资归属（REQ-61）。具体薪资单独填写，职级只提供薪资大范围。
 * 金额为整数分（D1），币种 P0 固定 CNY（D12）。隔离键继承自 access_users。
 */
public record StaffLevelAssignment(
        String userId,
        String staffLevelId,
        Long baseSalaryAmountCent,
        String salaryCurrency,
        String salaryDetail,
        String organizationId,
        String merchantId,
        String assignedBy,
        Instant assignedAt
) {
}
