package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CG-11b 经销订单到期作业（同 AmusementInspectionTaskJob 薄包装范式）：
 * E-13 首付款截止未达门槛取消（{@link DealerOrderService#cancelUnpaidAtDeadline}）与已出库尾款逾期标记（{@link DealerOrderService#markOverdue}）。
 * 默认关闭，dealer.order.deadline.scheduled.enabled=true 才注册。
 */
@Component
public class DealerOrderDeadlineJob {
    private final DealerOrderService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public DealerOrderDeadlineJob(DealerOrderService service, @Value("${dealer.order.deadline.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    DealerOrderDeadlineJob(DealerOrderService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "dealer.order.deadline.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${dealer.order.deadline.scheduled.fixed-delay-ms:300000}")
    public void runScheduled() {
        if (!scheduledEnabled) {
            return;
        }
        runAt(Instant.now(clock));
    }

    public void runAt(Instant now) {
        service.cancelUnpaidAtDeadline(now);
        service.markOverdue(now);
    }
}
