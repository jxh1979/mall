package com.complex.platform.amusement;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 游乐巡检到期调度作业（T3.1，同 TicketOrderExpiryJob/DashboardAggregationJob 模式）：
 * 业务逻辑在可直接调用的 {@link AmusementService#generateDueInspectionTasks(Instant)}，此处仅薄 @Scheduled 包装，
 * 便于单测直调对拍。定时任务通过 @ConditionalOnProperty 默认关闭（需显式开
 * amusement.inspection-task.scheduled.enabled=true 才注册），测试期不乱跑。
 *
 * <p>修复背景（深挖/审计 A4 相关）：V143 已建 amusement_inspection_tasks 表并承诺「按周期生成定期检修任务」，
 * 但生成调度此前一行未写（有表无逻辑），巡检到期只能在核销时被动拦截、无主动提醒。本作业激活该表。
 */
@Component
public class AmusementInspectionTaskJob {

    private final AmusementService service;
    private final Clock clock;

    @Autowired
    public AmusementInspectionTaskJob(AmusementService service) {
        this(service, Clock.systemUTC());
    }

    AmusementInspectionTaskJob(AmusementService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 amusement.inspection-task.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "amusement.inspection-task.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${amusement.inspection-task.scheduled.fixed-delay-ms:300000}")
    public void scheduledGenerateDueTasks() {
        service.generateDueInspectionTasks(Instant.now(clock));
    }
}
