package com.complex.platform.customer;

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
    private final MembershipService membershipService;
    private final Clock clock;

    @Autowired
    public LoyaltyScheduledJob(LoyaltyService service, MembershipService membershipService) {
        this(service, membershipService, Clock.systemUTC());
    }

    LoyaltyScheduledJob(LoyaltyService service, MembershipService membershipService, Clock clock) {
        this.service = service;
        this.membershipService = membershipService;
        this.clock = clock;
    }

    @ConditionalOnProperty(name = "customer.loyalty.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${customer.loyalty.scheduled.fixed-delay-ms:86400000}")
    public void scheduledEvaluate() {
        Instant now = Instant.now(clock);
        service.expirePoints(now);
        service.evaluateLevels(now);
        // 付费会员到期清理（V090 第2部分）：把已到期但仍 active 的订阅置 expired。
        membershipService.expireMemberships(now);
    }
}
