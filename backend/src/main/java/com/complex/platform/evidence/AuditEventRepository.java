package com.complex.platform.evidence;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    /**
     * 条目4 跨对象操作留痕查询：按业务类型/业务对象/操作人/事件名/时间范围过滤 audit_events（null 过滤项忽略），
     * 按发生时间倒序，最多 limit 条（上限 1000）。供审计人员跨对象搜索/导出操作日志。
     */
    default List<AuditEvent> searchOperationEvents(String businessType, String businessId, String actorId,
            String eventName, Instant from, Instant to, int limit, String merchantScope) {
        return List.of();
    }

    /** 第十八批 BA 段第 324 条 324-R1：projectScope 非 null 时只取操作人或其商户归属该项目集合的留痕（空集合取不到）。 */
    default List<AuditEvent> searchOperationEvents(String businessType, String businessId, String actorId,
            String eventName, Instant from, Instant to, int limit, String merchantScope, java.util.Set<String> projectScope) {
        return List.of();
    }
}
