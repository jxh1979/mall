package com.complex.platform.inventory;

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
        Instant updatedAt
) {
}
