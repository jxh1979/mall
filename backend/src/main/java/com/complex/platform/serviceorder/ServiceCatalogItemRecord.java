package com.complex.platform.serviceorder;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;

/** G批口径①:服务目录固定参考价(直付口径下仅作展示与台账,非平台营收)。V272 加软删字段(存档/删除申请审批)。
 * V518(TK-23-R3):fee_version=现行管理费版本;next_* 为待转正的管理费草案(拟改模式/数值、指定生效日、起草人、复核人)。 */
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
        String deleteReason,
        int feeVersion,
        String nextMgmtFeeMode,
        Long nextMgmtFeeValue,
        LocalDate nextEffectiveFrom,
        String nextRequestedBy,
        String nextApprovedBy
) {
    /** 便利构造:新建/更新时软删字段取默认(不存档、delete_status=none),费率版本 1、无草案。 */
    public ServiceCatalogItemRecord(String id, String name, long priceCent, Integer durationMinutes, boolean active,
            String mgmtFeeMode, long mgmtFeeValue, String updatedBy, Instant updatedAt) {
        this(id, name, priceCent, durationMinutes, active, mgmtFeeMode, mgmtFeeValue, updatedBy, updatedAt,
                null, null, null, "none", null, null, null, 1, null, null, null, null, null);
    }
}
