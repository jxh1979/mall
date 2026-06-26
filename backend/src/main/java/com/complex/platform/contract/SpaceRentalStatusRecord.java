package com.complex.platform.contract;

/** 铺位平面图着色数据源：每个铺位的租赁状态 + 命中的着色（已租/可租/即将到期）。 */
public record SpaceRentalStatusRecord(
        String spaceId,
        String spaceName,
        String rentalStatus,    // available(可租) / let(已租) / expiring(即将到期)
        String color,           // 命中着色分类色（仅 expiring 非空）
        Integer daysToExpiry    // 距到期天数（已租/即将到期非空）
) {
}
