package com.complex.platform.inventory;

// 文件版本：V0.01

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * FS-025 存酒转存（延期）流水（saved_bottle_renewals）。ENUM L120 / BSM L1072：每次转存固化当时的配置版本、顺延天数、
 * 新到期日与累计转存次数，加记经办人与客人确认方式；客人本人办理即生效（effective），员工代办须当班营销主管复核
 * （pending_review → effective 通过 / rejected 驳回：到期日不动、不计入累计转存次数，reviewReason 记驳回原因）。
 */
public record SavedBottleRenewalRecord(
        String id,
        String savedBottleId,
        @JsonIgnore String merchantId,
        String projectId,
        String customerId,
        @JsonIgnore String phoneHash,
        int extensionDays,
        Instant previousExpireAt,
        Instant newExpireAt,
        int renewalCount,
        int maxRenewals,
        String limitConfigVersion,
        String daysConfigVersion,
        String operatorId,
        String operationSource,
        String confirmationMethod,
        String reviewedBy,
        Instant reviewedAt,
        String reviewReason,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
