package com.complex.platform.inventory;

// 文件版本：V0.02

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * FS-017 门店退出存酒交接单（saved_bottle_handovers + saved_bottle_handover_items）。BSM L1072：
 * 「酒吧退出后，所属娱乐平台公司接收仍在保管有效期内的未结存酒实物和存酒记录，并指定领取地点，继续按原门店的保管规则和原有效期处理」
 * 「接收时已存在的毁损、短少……仍由原门店承担」——逐瓶记实收数量与状态（intact | damaged | short）。
 * 2026-10-20 操作员裁决「按差额」：short 另记 shortfallQuantity＝登记剩余量−实收量，为补存/损失/员工赔偿的数量。
 */
public record SavedBottleHandoverRecord(
        String id,
        @JsonIgnore String merchantId,
        String projectId,
        String platformEntityId,
        String pickupLocation,
        int countedTotal,
        int receivedIntact,
        int damagedOrShort,
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds,
        List<SavedBottleHandoverItemRecord> items,
        String createdBy,
        Instant createdAt
) {
    public record SavedBottleHandoverItemRecord(String savedBottleId, int receivedQuantity, String condition, int shortfallQuantity) {
    }
}
