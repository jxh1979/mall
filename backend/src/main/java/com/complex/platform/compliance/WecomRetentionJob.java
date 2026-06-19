package com.complex.platform.compliance;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * R5 T8 企微留存到期定时任务（实现同 DashboardAggregationJob：逻辑在可直接调用的
 * {@link WecomConversationService#applyRetention(LocalDate)}，此处仅薄 @Scheduled 包装）。
 *
 * <p>默认通过 @ConditionalOnProperty 关闭，测试期不会乱跑（需显式
 * compliance.wecom-retention.scheduled.enabled=true 才注册定时执行）。
 */
@Component
public class WecomRetentionJob {

    private final WecomConversationService service;
    private final Clock clock;

    @Autowired
    public WecomRetentionJob(WecomConversationService service) {
        this(service, Clock.systemUTC());
    }

    WecomRetentionJob(WecomConversationService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @ConditionalOnProperty(name = "compliance.wecom-retention.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${compliance.wecom-retention.scheduled.fixed-delay-ms:86400000}")
    public void scheduledApply() {
        service.applyRetention(LocalDate.ofInstant(Instant.now(clock), ZoneOffset.UTC));
    }
}
