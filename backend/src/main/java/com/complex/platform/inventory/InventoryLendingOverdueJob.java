package com.complex.platform.inventory;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批第 18 条 借用逾期未归还自动建工单定时作业（同包 SavedBottleNotificationJob 范式）：业务逻辑在
 * {@link InventoryTransferFlowService#raiseLendingOverdue(Instant)}，此处仅薄 @Scheduled 包装（BSM:1201「超过约定归还时间仍未归还的，
 * 系统自动生成一张工单并通知借出方主管」）。
 */
@Component
public class InventoryLendingOverdueJob {

    private final InventoryTransferFlowService flow;
    private final boolean scheduledEnabled;

    public InventoryLendingOverdueJob(
            InventoryTransferFlowService flow,
            @Value("${inventory.lending-overdue.scheduled.enabled:false}") boolean scheduledEnabled
    ) {
        this.flow = flow;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 inventory.lending-overdue.scheduled.enabled=true 时执行。 */
    @ConditionalOnProperty(name = "inventory.lending-overdue.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${inventory.lending-overdue.scheduled.fixed-delay-ms:300000}")
    public void scheduledRaiseLendingOverdue() {
        if (scheduledEnabled) {
            flow.raiseLendingOverdue(Instant.now());
        }
    }
}
