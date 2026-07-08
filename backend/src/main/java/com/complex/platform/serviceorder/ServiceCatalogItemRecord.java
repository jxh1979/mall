package com.complex.platform.serviceorder;

import java.time.Instant;

/** G批口径①:服务目录固定参考价(直付口径下仅作展示与台账,非平台营收)。V272 加软删字段(存档/删除申请审批)。 */
public record ServiceCatalogItemRecord(
        String id,
        String name,
        long priceCent,
        Integer durationMinutes,
        boolean active,
        String mgmtFeeMode,
        long mgmtFeeValue,
        String updatedBy,
        Instant updatedAt,
        Instant archivedAt,
        String archivedBy,
        String archiveReason,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason
) {
    /** 便利构造:新建/更新时软删字段取默认(不存档、delete_status=none)。 */
    public ServiceCatalogItemRecord(String id, String name, long priceCent, Integer durationMinutes, boolean active,
            String mgmtFeeMode, long mgmtFeeValue, String updatedBy, Instant updatedAt) {
        this(id, name, priceCent, durationMinutes, active, mgmtFeeMode, mgmtFeeValue, updatedBy, updatedAt,
                null, null, null, "none", null, null, null);
    }
}
