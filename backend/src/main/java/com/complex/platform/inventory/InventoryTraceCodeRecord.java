package com.complex.platform.inventory;

// 文件版本：V0.02

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * V128 / REQ-39 溯源码主数据（inventory_trace_codes）。
 *
 * <p>承载“酒入库贴码（登记）→ 出库打印码 → 二维码串责任链查询”闭环的码主数据，与扫码交接（V074
 * TraceHandoverRecord）互补：本表是码本身的生命周期主数据，交接链是码被扫描后的责任流转。
 *
 * <p>{@code status} 生命周期：registered（贴码登记/入库）| printed（出库打印）| bound（绑定单据）|
 * consumed（核销）。{@code traceCode} 码值唯一。{@code merchantId} 直接归属列，强制取自认证上下文，归属隔离
 * （供 domain-ownership 自动归类 direct scope）。{@code printedAt} 打印时回填；{@code boundRef} 出库绑定单据引用。
 *
 * <p>FS-019（BSM L1201「溯源码同时记录所有权主体、当前保管主体、借出主体、借入主体、应归还数量和期限」）：
 * {@code custodianType/custodianId} 为当前保管主体（空＝即所有权人），{@code lendingOrderId} 为在借的借用单
 * （借出/借入主体、应归还数量与期限在该单上），归还或实物处置完成后置空。
 */
public record InventoryTraceCodeRecord(
        String id,
        String traceCode,
        String itemId,
        String batchId,
        @JsonIgnore String merchantId,
        String status,
        String boundRef,
        Instant printedAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        String custodianType,
        String custodianId,
        String lendingOrderId
) {
    /** 登记/打印：保管人即所有权人、不在借。 */
    public InventoryTraceCodeRecord(String id, String traceCode, String itemId, String batchId, String merchantId, String status,
            String boundRef, Instant printedAt, String createdBy, Instant createdAt, Instant updatedAt) {
        this(id, traceCode, itemId, batchId, merchantId, status, boundRef, printedAt, createdBy, createdAt, updatedAt, null, null, null);
    }
}
