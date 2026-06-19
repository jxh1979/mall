package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购单记录。
 *
 * <p>R5 T6 采购周期状态机：status ∈
 * {@code created/submitted → approved/rejected → ordered → expected → arrived → received}。
 * 起点兼容既有写入——createPurchaseRequest 继续写 {@code created}（视为 submitted 同义起点），
 * 流转入口（approve）从 {@code created} 或 {@code submitted} 均可进入审批。
 *
 * <p>流转字段（V085 扩列）：supplyContractId 采购引用的供应合同（卡控用）；approvedBy/orderedAt/
 * actualArrivalDate/receivedAt/receivedBy 分别记录审批/下单/到货/入库各步的执行信息。
 */
public record PurchaseRequestRecord(
        String id,
        String requestNo,
        String supplierId,
        String itemId,
        int quantity,
        LocalDate expectedArrivalDate,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        @JsonIgnore String merchantId,
        // R5 T6 采购周期状态机扩列（V085）。
        String supplyContractId,
        String approvedBy,
        Instant orderedAt,
        LocalDate actualArrivalDate,
        Instant receivedAt,
        String receivedBy
) {
}
