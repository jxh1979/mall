package com.complex.platform.access.staff.dto;

/**
 * 绑定 / 换机申请。device_public_key=Android Keystore 设备公钥；
 * device_fingerprint_hash=不可逆设备指纹哈希（不读 NFC，D8）。
 * 若该员工已有 active 凭证且指纹不同 → 视为换机：新凭证 pending_review + supersedes 指向旧 active。
 */
public record StaffDeviceBindRequest(
        String idempotencyKey,
        String operatorNote,
        String staffProfileId,
        String devicePublicKey,
        String deviceFingerprintHash
) {
}
