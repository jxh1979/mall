package com.complex.platform.amusement;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CG-03 在玩人数系统自动清零调度作业（同 AmusementInspectionTaskJob 薄包装范式）：
 * 业务逻辑在 {@link AmusementService#closeEndedSessionsAndClearCounts(Instant)}（场次结束自动清空）与
 * {@link AmusementService#resetConcurrentCountsAtBusinessDayEnd(Instant)}（营业日结束自动清零），此处仅 @Scheduled 包装，
 * 便于单测直调对拍。默认关闭（需 amusement.concurrent-reset.scheduled.enabled=true 才注册），测试期不乱跑。
 * 计数只能由入场核销、出场自动扣减与本作业产生：不提供任何人工入口。
 */
@Component
public class AmusementConcurrentCountResetJob {

    private final AmusementService service;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public AmusementConcurrentCountResetJob(
            AmusementService service,
            @Value("${amusement.concurrent-reset.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, Clock.systemUTC(), scheduledEnabled);
    }

    AmusementConcurrentCountResetJob(AmusementService service, Clock clock) {
        this(service, clock, true);
    }

    AmusementConcurrentCountResetJob(AmusementService service, Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "amusement.concurrent-reset.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${amusement.concurrent-reset.scheduled.fixed-delay-ms:60000}")
    public void scheduledRun() {
        if (!scheduledEnabled) {
            return;
        }
        runAt(Instant.now(clock));
    }

    /** 先按场次结束清空，再按营业日结束清零；两者各自幂等。返回 {置 closed 并清空的场次数, 新清零的项目数}。 */
    public int[] runAt(Instant now) {
        int sessions = service.closeEndedSessionsAndClearCounts(now);
        int projects = service.resetConcurrentCountsAtBusinessDayEnd(now);
        return new int[] {sessions, projects};
    }
}
