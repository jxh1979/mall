package com.complex.platform.finance;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 R 段第 143 条 143-R3 日结五类数据逾期未标记自动发待办的定时作业（ENUM:190「某一类数据在上述时点仍未标记的，系统自动生成待办并通知
 * 该域主管公司的财务负责人」；第十八批裁决 35）。同 {@link com.complex.platform.facility.FacilityWorkOrderEscalationJob} 范式：业务规则在
 * {@link FinanceFinalMarkOverdueService#scanFinalMarkOverdue}，这里只做 @Scheduled 薄包装，默认关闭，prod 开启（默认每小时一轮）。
 */
@Component
public class FinanceFinalMarkOverdueJob {

    private final FinanceFinalMarkOverdueService overdueService;
    private final boolean scheduledEnabled;

    public FinanceFinalMarkOverdueJob(FinanceFinalMarkOverdueService overdueService,
            @Value("${finance.final-mark-overdue.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.overdueService = overdueService;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "finance.final-mark-overdue.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${finance.final-mark-overdue.scheduled.fixed-delay-ms:3600000}")
    public void scheduledScan() {
        if (scheduledEnabled) {
            overdueService.scanFinalMarkOverdue(Instant.now());
        }
    }
}
