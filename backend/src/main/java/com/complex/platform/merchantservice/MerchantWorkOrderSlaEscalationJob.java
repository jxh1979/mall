package com.complex.platform.merchantservice;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 物业工单 SLA 自动超时升级定时作业（同 DashboardAggregationJob/LoyaltyScheduledJob 模式）：
 * 业务逻辑在可直接调用的 {@link MerchantServiceService#escalateOverdueWorkOrders(Instant)}，
 * 此处仅薄 @Scheduled 包装调用它——便于单测直接调方法对拍业务表，不依赖定时触发。
 * 定时任务通过 @ConditionalOnProperty 默认关闭，测试期不会乱跑
 * （需显式开 merchant.service.sla-escalation.scheduled.enabled=true 才注册）。
 */
@Component
public class MerchantWorkOrderSlaEscalationJob {

    private final MerchantServiceService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public MerchantWorkOrderSlaEscalationJob(
            MerchantServiceService service,
            @Value("${merchant.service.sla-escalation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    MerchantWorkOrderSlaEscalationJob(MerchantServiceService service, Clock clock) {
        this(service, clock, true);
    }

    MerchantWorkOrderSlaEscalationJob(MerchantServiceService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 merchant.service.sla-escalation.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "merchant.service.sla-escalation.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${merchant.service.sla-escalation.scheduled.fixed-delay-ms:300000}")
    public void scheduledEscalate() {
        if (!scheduledEnabled) {
            return;
        }
        service.escalateOverdueWorkOrders(Instant.now(clock));
    }
}
