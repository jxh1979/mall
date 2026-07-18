package com.complex.platform.customer;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 会员忠诚度定时作业（同 WecomRetentionJob 模式）：逻辑在可直接调用的
 * {@link LoyaltyService#evaluateLevels(Instant)} / {@link LoyaltyService#expirePoints(Instant)}，
 * 此处仅薄 @Scheduled 包装。默认 @ConditionalOnProperty 关闭，测试期不会乱跑。
 */
@Component
public class LoyaltyScheduledJob {

    private final LoyaltyService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public LoyaltyScheduledJob(
            LoyaltyService service,
            @Value("${customer.loyalty.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    LoyaltyScheduledJob(LoyaltyService service, Clock clock) {
        this(service, clock, true);
    }

    LoyaltyScheduledJob(LoyaltyService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "customer.loyalty.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${customer.loyalty.scheduled.fixed-delay-ms:86400000}")
    public void scheduledEvaluate() {
        if (!scheduledEnabled) {
            return;
        }
        Instant now = Instant.now(clock);
        service.expirePoints(now);
        service.evaluateLevels(now);
    }
}
