package com.complex.platform.serviceorder.dto;

// 文件版本：V0.01

/** 第十一批 TK-04-R2：memberPhone 可选——收款时录入会员号（即注册手机号）识别本单会员（BSM L822、PGM L878）。 */
public record ServiceOrderMarkPaidRequest(
        String idempotencyKey,
        String payMethod,
        String memberPhone
) {
    public ServiceOrderMarkPaidRequest(String idempotencyKey, String payMethod) {
        this(idempotencyKey, payMethod, null);
    }
}
