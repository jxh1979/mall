package com.complex.platform.serviceorder;

import java.time.Instant;

/** G批口径①:服务目录固定参考价(直付口径下仅作展示与台账,非平台营收)。 */
public record ServiceCatalogItemRecord(
        String id,
        String name,
        long priceCent,
        Integer durationMinutes,
        boolean active,
        String updatedBy,
        Instant updatedAt
) {
}
