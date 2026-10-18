package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;

/** 经销商门户账号归属（access_users.account_type = dealer）。status：active / revoked（停用撤权）。 */
public record DealerPortalAccountRecord(String userId, String dealerId, String username, String status, String createdBy, Instant createdAt, Instant revokedAt) {
}
