package com.complex.platform.access.staff.dto;

// 文件版本：V0.02
/** U05-003（PGM L579/BSM L935）：私人手机 App 安装时生成的设备公钥（Base64 X.509 DER）与指纹（SHA-256(DER) 十六进制）随注册提交并绑定本人。 */
public record StaffSelfRegistrationRequest(String idempotencyKey, String companyId, String merchantId,
        String name, String phone, String idDoc, String devicePublicKey, String deviceFingerprintHash) {}
