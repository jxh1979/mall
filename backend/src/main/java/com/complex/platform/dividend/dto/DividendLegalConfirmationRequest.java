package com.complex.platform.dividend.dto;

// 文件版本：V0.01

/**
 * FS-014 财务登记依法可分配金额（BSM L1785「由财务依据专业财税核算结果及公司有效分配决议，确认依法可分配金额」）：
 * 金额为分；财税核算依据与分配决议必填。
 */
public record DividendLegalConfirmationRequest(
        String idempotencyKey,
        Long legalDistributableCent,
        String taxAccountingRef,
        String resolutionRef,
        String note) {
}
