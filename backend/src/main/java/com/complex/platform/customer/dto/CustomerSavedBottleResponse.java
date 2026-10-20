package com.complex.platform.customer.dto;

// 文件版本：V0.01

import com.complex.platform.inventory.SavedBottleRecord;
import java.time.Instant;

/**
 * 客人端「我的存酒」安全投影（BSM L867「查看本人名下全部存酒、到期日与动态取酒码……跨店汇总展示，每条记录标明所属酒吧」）：
 * 只下发客人可见字段；刻意不含员工备注、高价值标记与阈值、登记照片、保管主体/交接单等员工内部字段。
 */
public record CustomerSavedBottleResponse(
        String id,
        String barProjectId,
        String bottleName,
        int remainingQuantity,
        String unit,
        Instant expireAt,
        String status,
        String bottleLabelValue,
        String pickupLocation,
        boolean handedOverToPlatform
) {
    public static CustomerSavedBottleResponse from(SavedBottleRecord bottle) {
        return new CustomerSavedBottleResponse(bottle.id(), bottle.barProjectId(), bottle.bottleName(),
                bottle.remainingQuantity(), bottle.unit(), bottle.expireAt(), bottle.status(), bottle.bottleLabelValue(),
                bottle.pickupLocation(), bottle.custodianEntityId() != null);
    }
}
