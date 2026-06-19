package com.complex.platform.access.staff;

import java.time.Instant;

/**
 * 员工私人手机设备凭证（R5 T1）。Android Keystore 设备公钥 + 不可逆设备指纹哈希。
 * <p><b>关键边界</b>：与 kiosk 的 terminal_device_credentials（V011，随机 token）是两套完全不同实体，
 * 互不复用、互不串值、无外键关联——本表外键仅指向 staff_profiles 与自身（换机 supersedes 链）。
 * bind_status: pending_review/active/superseded/revoked；
 * integrity_status: trusted/root_detected/debug_detected/abnormal。
 * 唯一 active：每个员工至多一条 bind_status='active'。
 */
public record StaffDeviceCredentialRecord(
        String id,
        String staffProfileId,
        String devicePublicKey,
        String deviceFingerprintHash,
        String bindStatus,
        String supersedesCredentialId,
        String integrityStatus,
        String revokedReason,
        Instant boundAt,
        Instant revokedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
