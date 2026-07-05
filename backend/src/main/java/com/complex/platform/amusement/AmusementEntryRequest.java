package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 阶段1 游乐核销分流入参（ticketing → {@link AmusementVerificationPort}）。
 *
 * @param credentialPayload 核销凭证 payload（动态二维码 token 或权益码）——游乐正路据此识别票/权益实例
 * @param pointId           核销点位（用于 point→项目定位、点位维度容量）；可空
 * @param deviceId          核销设备（用于 device→项目定位、设备维度容量、设备巡检有效期）；可空
 * @param staffId           操作员（落 amusement_verifications.staff_id）；可空回退 "staff"
 * @param idempotencyKey    幂等键（用于 amusement_verifications.id 稳定生成，重放不重复扣容量）
 * @param now               核销时刻（当前场次定位与 verified_at）
 */
public record AmusementEntryRequest(
        String credentialPayload,
        String pointId,
        String deviceId,
        String staffId,
        String idempotencyKey,
        Instant now
) {
}
