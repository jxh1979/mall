package com.complex.platform.customer;

import java.time.Instant;

/**
 * 储值余额账户（gap-analysis §7A P1：预付卡 / 余额账户）。
 * balanceCent=当前余额（分）；status=active|frozen|closed。
 * <p>B1d⑤ 储值卡分级：{@code merchantId} 为空=综合体通用卡（综合体内各商户通用，按 project 隔离）；
 * {@code merchantId} 非空=商户级卡（只能在该发卡商户消费，按 merchant 隔离）。卡范围唯一键=(customer, project, merchant)。
 */
public record CustomerStoredValueAccountRecord(
        String id,
        String customerId,
        String projectId,
        String merchantId,
        long balanceCent,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    /** 卡类型：merchant=商户级卡，project=综合体通用卡。 */
    public String cardScope() {
        return merchantId == null ? "project" : "merchant";
    }

    /** 向后兼容构造（综合体通用卡，merchantId=null）：保留 V175 前调用方零改动。 */
    public CustomerStoredValueAccountRecord(
            String id,
            String customerId,
            String projectId,
            long balanceCent,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, customerId, projectId, null, balanceCent, status, createdAt, updatedAt);
    }
}
