package com.complex.platform.amusement.dto;

/** CG-08a 第二岗位确认（生效或解除）：role＝确认人本次所用岗位，须与发起岗位不同且为不同人。 */
public record ParkWideFaultConfirmRequest(
        String idempotencyKey,
        String role,
        String note
) {
}
