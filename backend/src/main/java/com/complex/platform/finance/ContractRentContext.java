package com.complex.platform.finance;

/**
 * ② 履约闭环 S2：合同租金计费所需的只读上下文（finance 跨域只读 contracts，仅取计费必要字段）。
 * baseRentCent 来自 rent_terms->>'baseRentCent'（分）；缺该键则为 null（该合同跳过计费，不猜单位）。
 */
public record ContractRentContext(
        String merchantId,
        String lifecycleStatus,
        Long baseRentCent
) {
}
