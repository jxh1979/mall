package com.complex.platform.inventory;

// 文件版本：V0.03

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record InventoryItemRecord(
        String id,
        String skuCode,
        String itemName,
        String itemType,
        String unit,
        String barcode,
        boolean traceRequired,
        int minQuantity,
        String status,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId,
        // V350 归一：物品档案「手工标记为高价值」。TRUE = 不论进货价一律按高价值走复核/先审后行。
        // 取代此前的按物料阈值 high_value_threshold_cents（与「全平台一个固定金额阈值」冲突，V350 已删）。
        boolean manualHighValue,
        // FS-019（BSM L1201「「借」只转移保管责任……仅适用于不可消费物品；可消费物品的借用实质是调」）：
        // 不可消费物品（设备、器具等）才能借用；出厂按可消费从严。
        boolean nonConsumable,
        // 第十八批 I 段第 76 条（ENUM:79「定价、库存、结算、对账一律按商品规格逐项进行」；BSM:1065「按杯按标准出酒量扣库存」）：
        // 规格容量（毫升），如 700 毫升瓶装填 700；按杯按容量扣库存的物料须登记，不按容量扣的留空。
        Integer specVolumeMl
) {
    public InventoryItemRecord(String id, String skuCode, String itemName, String itemType, String unit, String barcode,
            boolean traceRequired, int minQuantity, String status, Instant createdAt, Instant updatedAt, String merchantId,
            boolean manualHighValue) {
        this(id, skuCode, itemName, itemType, unit, barcode, traceRequired, minQuantity, status, createdAt, updatedAt, merchantId,
                manualHighValue, false, null);
    }

    /** 不带规格容量的写法（手工标记高价值、改安全库存重建记录再落库）：容量置空，落库 upsert 的更新段不改该列。 */
    public InventoryItemRecord(String id, String skuCode, String itemName, String itemType, String unit, String barcode,
            boolean traceRequired, int minQuantity, String status, Instant createdAt, Instant updatedAt, String merchantId,
            boolean manualHighValue, boolean nonConsumable) {
        this(id, skuCode, itemName, itemType, unit, barcode, traceRequired, minQuantity, status, createdAt, updatedAt, merchantId,
                manualHighValue, nonConsumable, null);
    }
}
