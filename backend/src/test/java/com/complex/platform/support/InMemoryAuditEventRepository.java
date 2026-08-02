package com.complex.platform.support;

import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import java.util.ArrayList;
import java.util.List;

/** 测试共享的内存版审计事件仓储：收敛自各测试类中逐字相同的本地副本；events 公开供测试直接断言。 */
public final class InMemoryAuditEventRepository implements AuditEventRepository {

    public final List<AuditEvent> events = new ArrayList<>();

    @Override
    public AuditEvent save(AuditEvent event) {
        events.add(event);
        return event;
    }
}
