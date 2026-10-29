package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第 77 条 77-R1 物业工单响应／处理超时自动升级通知主管定时作业（BSM:1382、BSM:1385、ENUM:265）。同 {@link FacilityAlarmEscalationJob} 范式：
 * 业务规则在 {@link FacilityWorkOrderService#escalateOverdue}，这里只做 @Scheduled 薄包装，默认关闭，prod 开启（默认每分钟一轮）。
 */
@Component
public class FacilityWorkOrderEscalationJob {

    private final FacilityWorkOrderService workOrderService;
    private final boolean scheduledEnabled;

    public FacilityWorkOrderEscalationJob(FacilityWorkOrderService workOrderService,
            @Value("${facility.work-order-escalation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.workOrderService = workOrderService;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "facility.work-order-escalation.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${facility.work-order-escalation.scheduled.fixed-delay-ms:60000}")
    public void scheduledEscalate() {
        if (scheduledEnabled) {
            escalate(Instant.now());
        }
    }

    /** 对 now 时刻已过响应或处理时限、该类还没升级过的工单升级并通知主管；返回本轮升级的工单数。 */
    int escalate(Instant now) {
        return workOrderService.escalateOverdue(now);
    }
}
