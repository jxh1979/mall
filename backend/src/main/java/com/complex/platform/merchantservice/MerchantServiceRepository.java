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

    MerchantWorkOrderActionRecord saveAction(MerchantWorkOrderActionRecord record);

    List<MerchantWorkOrderActionRecord> findActionsByWorkOrderId(String workOrderId);
}
