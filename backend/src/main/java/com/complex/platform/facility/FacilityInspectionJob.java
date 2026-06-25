package com.complex.platform.facility;

import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 楼宇巡检到期调度作业（③ S3，仿 AmusementInspectionTaskJob）：业务逻辑在可直接调用的
 * {@link FacilityService#generateDueInspectionTasks(Instant)}，此处仅薄 @Scheduled 包装，便于单测直调对拍。
 * 通过 @ConditionalOnProperty 默认关闭（需显式开 facility.inspection.scheduled.enabled=true 才注册），测试期不乱跑。
 * 多构造器必标 @Autowired 指定注入构造器（@WebMvcTest 测不出、全上下文才暴露，② 踩过）。
 */
@Component
public class FacilityInspectionJob {

    private final FacilityService service;
    private final Clock clock;

    @Autowired
    public FacilityInspectionJob(FacilityService service) {
        this(service, Clock.systemUTC());
    }

    FacilityInspectionJob(FacilityService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @ConditionalOnProperty(name = "facility.inspection.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${facility.inspection.scheduled.fixed-delay-ms:300000}")
    public void scheduledGenerateDueTasks() {
        service.generateDueInspectionTasks(Instant.now(clock));
    }
}
