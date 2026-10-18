package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/** 隔离数量包含在同一 stock_balance.quantity 内，不能再次计入总库存。 */
public record InventoryQuarantineLotRecord(String id,String stockBalanceId,String itemId,String warehouseId,String projectId,
        String sourceType,String sourceId,int quantity,int remainingQuantity,List<String> evidenceAttachmentIds,String receivedBy,Instant receivedAt) {}
