package com.complex.platform.finance;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 Y 段第 181 条 181-R2 对账差异待识别、待处理超时提醒与升级定时作业（ENUM:191「超时未推进的，系统自动提醒并升级到上一级负责人。」）。
 * 同 {@link com.complex.platform.facility.FacilityWorkOrderEscalationJob} 范式：业务规则在
 * {@link FinanceReconciliationDifferenceService#escalateOverdue}，这里只做 @Scheduled 薄包装，默认关闭，prod 开启（默认每分钟一轮）。
 */
@Component
public class FinanceReconciliationEscalationJob {

    private final FinanceReconciliationDifferenceService differences;
    private final boolean scheduledEnabled;

    public FinanceReconciliationEscalationJob(FinanceReconciliationDifferenceService differences,
            @Value("${finance.reconciliation-escalation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.differences = differences;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "finance.reconciliation-escalation.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${finance.reconciliation-escalation.scheduled.fixed-delay-ms:60000}")
    public void scheduledEscalate() {
        if (scheduledEnabled) {
            differences.escalateOverdue(Instant.now());
        }
    }
}
