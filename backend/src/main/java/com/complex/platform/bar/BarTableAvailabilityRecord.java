package com.complex.platform.bar;

/** BYOD 一期:酒吧空位视图行——员工自有手机帮客人订位用(桌况+活跃预订占用标记)。 */
public record BarTableAvailabilityRecord(
        String id,
        String barId,
        String tableCode,
        String tableName,
        String tableType,
        int capacity,
        long minConsumptionCent,
        String status,
        boolean reserved
) {
}
