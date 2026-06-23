package com.complex.platform.finance;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * F-OPS-08 物业费逐户计费留痕（不退不补；UNIQUE(projectId,period,merchantId) 幂等）。
 * amountCent = round(ratePerSquareMeterCent × areaSquareMeter)，并联动生成 source_type='property_fee' 应收 ar_ap_items。
 */
public record PropertyFeeBillRecord(
        String id,
        String projectId,
        String period,
        String merchantId,
        BigDecimal areaSquareMeter,
        long ratePerSquareMeterCent,
        long amountCent,
        String arApItemId,
        Instant createdAt
) {
}
