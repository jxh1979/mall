package com.complex.platform.ticketing;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 票务未支付订单超时释放库存定时作业（2a-5，同 MerchantWorkOrderSlaEscalationJob/DashboardAggregationJob 模式）：
 * 业务逻辑在可直接调用的 {@link TicketingService#cancelExpiredUnpaidTicketOrders(Instant)}，此处仅薄 @Scheduled 包装，
 * 便于单测直接调方法对拍业务表，不依赖定时触发。定时任务通过 @ConditionalOnProperty 默认关闭，测试期不乱跑
 * （需显式开 ticket.order.expiry.scheduled.enabled=true 才注册）。
 *
 * <p>修复背景：下单即扣库存 + 未支付订单永卡 unpaid（无回写/无取消）→ 限量库存被未支付订单永久锁死。
 * 本作业按 ticket.order.expiry.unpaid-timeout-minutes（默认 30 分钟）超时阈值取消未支付订单并回补库存。
 */
@Component
public class TicketOrderExpiryJob {

    private final TicketingService service;
    private final Clock clock;
    private final long timeoutMinutes;
    private final boolean scheduledEnabled;

    @Autowired
    public TicketOrderExpiryJob(TicketingService service,
                                @Value("${ticket.order.expiry.unpaid-timeout-minutes:30}") long timeoutMinutes,
                                @Value("${ticket.order.expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), timeoutMinutes, scheduledEnabled);
    }

    TicketOrderExpiryJob(TicketingService service, Clock clock, long timeoutMinutes) {
        this(service, clock, timeoutMinutes, true);
    }

    TicketOrderExpiryJob(TicketingService service, Clock clock, long timeoutMinutes, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.timeoutMinutes = timeoutMinutes;
        this.scheduledEnabled = scheduledEnabled;
    }

    /**
     * @Scheduled 薄包装：默认关闭，仅在 ticket.order.expiry.scheduled.enabled=true 时注册。
     * 两件事：① 取消超时未支付订单回补库存（按 unpaid-timeout 宽限）；② F-TXN-03 门票生命周期——
     * 处理已过期、全未核销的权益（全未核销自动退款 / 一张用后其余作废，按权益 valid_to 实时判定）。
     */
    @ConditionalOnProperty(name = "ticket.order.expiry.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${ticket.order.expiry.scheduled.fixed-delay-ms:300000}")
    public void scheduledReleaseExpired() {
        if (!scheduledEnabled) {
            return;
        }
        service.cancelExpiredUnpaidTicketOrders(Instant.now(clock).minus(Duration.ofMinutes(timeoutMinutes)));
        service.processExpiredPaidEntitlements(Instant.now(clock));
    }
}
