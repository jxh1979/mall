package com.complex.platform.amusement;

/**
 * 领取核销权益会话请求体（V129/REQ-53，一体机领取）。
 *
 * <p>idempotencyKey：幂等键（必填，重复领取返回同一结果）。
 * claimedBy：领取核销操作员/一体机标识（可空，缺省取系统占位）。
 * 防错领由服务端条件 UPDATE 保证（仅 initiated/displayed 且未过期可领），不依赖请求体。
 */
public record ClaimEntitlementSessionRequest(
        String idempotencyKey,
        String claimedBy
) {
}
