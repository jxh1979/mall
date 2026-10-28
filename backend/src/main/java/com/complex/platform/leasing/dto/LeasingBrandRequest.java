package com.complex.platform.leasing.dto;

/** 品牌库维护请求（V161 重建）。brandType 4 类：intentional/prospective/partner/blacklist。 */
public record LeasingBrandRequest(
        String brandName,
        String brandType,
        String businessType,
        String idempotencyKey,
        // 18E（第 19 条 19-R2）：落地项目（可选）；不带取本人项目
        String projectId
) {
    public LeasingBrandRequest(String brandName, String brandType, String businessType, String idempotencyKey) {
        this(brandName, brandType, businessType, idempotencyKey, null);
    }
}
