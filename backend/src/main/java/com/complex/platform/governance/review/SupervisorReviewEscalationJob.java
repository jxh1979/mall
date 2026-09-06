package com.complex.platform.governance.review;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * M007-A-02（PGM-1.7.1「任一业务审批节点超过后台配置的时限……系统自动提醒并升级到该节点的上一级，不得默认通过，也不得默认拒绝」）：
 * 同 ContractExpiryJob 范式的薄 @Scheduled 包装；业务逻辑在 {@link SupervisorReviewService#escalateOverdue(Instant)}，便于单测直接调用。
 */
@Component
public class SupervisorReviewEscalationJob {
    private final SupervisorReviewService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public SupervisorReviewEscalationJob(SupervisorReviewService service,
            @Value("${governance.supervisor-review.escalation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    SupervisorReviewEscalationJob(SupervisorReviewService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 governance.supervisor-review.escalation.scheduled.enabled=true 时执行（每小时）。 */
    @ConditionalOnProperty(name = "governance.supervisor-review.escalation.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${governance.supervisor-review.escalation.scheduled.fixed-delay-ms:3600000}")
    public void scheduledEscalateOverdue() {
        if (!scheduledEnabled) {
            return;
        }
        runOnce(Instant.now(clock));
    }

    public int runOnce(Instant now) {
        return service.escalateOverdue(now);
    }
}
