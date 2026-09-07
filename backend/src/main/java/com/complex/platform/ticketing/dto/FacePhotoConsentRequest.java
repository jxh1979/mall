package com.complex.platform.ticketing.dto;

/**
 * R-005（操作员 2026-09-07）：柜台出票现场为无会员身份的散客留存人脸照同意。
 * 客人在柜台设备上看过告知（用途/保存方式/留存天数/不拍的后果）并勾选同意后，由出票人员提交本请求；
 * 服务端以订单为主体落一条 customer_consents（consent_type=ticket_face_photo，版本=当时那一版告知），
 * 返回同意 id，随后出票请求（face_photos + face_consent_id）带上它。accepted=false 或缺失一律拒，不落任何行。
 */
public record FacePhotoConsentRequest(
        String idempotencyKey,
        Boolean accepted,
        // 采集终端标识（如 counter / kiosk-3），只作留痕；缺省记 counter。
        String sourceTerminal
) {
}
