package com.complex.platform.facility;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * HP-F01 R5/R6 设备告警超时升级与换班交接定时作业（BSM「楼宇运营」告警条 L1417）。同 BarPendingClearAlertJob 范式：
 * 业务规则在 {@link FacilityAlarmService#escalate}，这里只做 @Scheduled 薄包装，默认关闭，prod 开启（默认每分钟一轮）。
 */
@Component
public class FacilityAlarmEscalationJob {

    private final FacilityAlarmService alarmService;
    private final boolean scheduledEnabled;

    public FacilityAlarmEscalationJob(FacilityAlarmService alarmService,
            @Value("${facility.alarm-escalation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.alarmService = alarmService;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "facility.alarm-escalation.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${facility.alarm-escalation.scheduled.fixed-delay-ms:60000}")
    public void scheduledEscalate() {
        if (scheduledEnabled) {
            escalate(Instant.now());
        }
    }

    /** 对 now 时刻到期的待接手告警升级、对接手人班次已结束的未结告警发起交接；返回本轮处理的接警单数。 */
    int escalate(Instant now) {
        return alarmService.escalate(now);
    }
}
