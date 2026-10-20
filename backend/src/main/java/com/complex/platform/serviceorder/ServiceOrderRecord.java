package com.complex.platform.serviceorder;

// 文件版本：V0.01

import java.time.Instant;

/** G批:服务单——下单即指定履约人(口径③);价格为目录快照参考价(口径④直付,不入账)。
 * TK-23-R3:snap_fee_version 记下单时锁定的管理费版本。 */
public record ServiceOrderRecord(
        String id,
        String catalogItemId,
        String itemName,
        long priceCent,
        String barId,
        String tableId,
        String performerUserId,
        String orderedBy,
        String customerNote,
        String status,
        String cancelReason,
        Instant paidAt,
        String paidBy,
        String payMethod,
        String refundNote,
        long mgmtFeeCent,
        long dancerShareCent,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        String snapMgmtFeeMode,
        Long snapMgmtFeeValue,
        Integer snapFeeVersion
) {
    /** 兼容构造(无费率版本):既有状态流转写入方不变,snap_fee_version 置 null(upsert 冲突时不改该列)。 */
    public ServiceOrderRecord(String id, String catalogItemId, String itemName, long priceCent, String barId,
            String tableId, String performerUserId, String orderedBy, String customerNote, String status,
            String cancelReason, Instant paidAt, String paidBy, String payMethod, String refundNote,
            long mgmtFeeCent, long dancerShareCent, Instant createdAt, Instant updatedAt, Instant completedAt,
            String snapMgmtFeeMode, Long snapMgmtFeeValue) {
        this(id, catalogItemId, itemName, priceCent, barId, tableId, performerUserId, orderedBy, customerNote,
                status, cancelReason, paidAt, paidBy, payMethod, refundNote, mgmtFeeCent, dancerShareCent,
                createdAt, updatedAt, completedAt, snapMgmtFeeMode, snapMgmtFeeValue, null);
    }
}
