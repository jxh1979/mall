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
            String eventName, Instant from, Instant to, int limit) {
        return List.of();
    }
}
