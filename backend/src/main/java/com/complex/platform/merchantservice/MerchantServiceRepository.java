package com.complex.platform.merchantservice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MerchantServiceRepository {
    List<MerchantWorkOrderRecord> findWorkOrdersByScope(Optional<String> merchantScope, int limit);

    Optional<MerchantWorkOrderRecord> findWorkOrderById(String id);

    /**
     * SLA 扫描：返回 due_at 已过期（&lt;= now）、未完成（status 非 resolved/closed/cancelled）
     * 且尚未升级（escalated_at IS NULL）的工单，供定时升级任务处理。
     */
    List<MerchantWorkOrderRecord> findOverdueOpenWorkOrders(Instant now, int limit);

    MerchantWorkOrderRecord saveWorkOrder(MerchantWorkOrderRecord record);

    /**
     * SLA 条件升级（并发幂等护栏）：仅当该工单 escalated_at IS NULL 时，原子地打标
     * sla_breached=true、escalated_at=now、updated_at=now，并返回受影响行数（1 表示本调用赢得升级，
     * 0 表示已被其它并发事务（定时 vs 手动触发）抢先升级，应跳过留痕避免双重升级/双留痕）。
     * 不改变 status（升级是横切标记，非状态迁移）。
     */
    int markSlaBreachedIfNotEscalated(String id, java.time.Instant now);

    MerchantWorkOrderActionRecord saveAction(MerchantWorkOrderActionRecord record);

    List<MerchantWorkOrderActionRecord> findActionsByWorkOrderId(String workOrderId);
}
