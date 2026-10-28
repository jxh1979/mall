// 文件版本：V0.01
package com.complex.platform.compliance;

import com.complex.platform.common.LocalZone;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 E 段第 22 条：证照到期预警、到期与宽限期每日提醒的自动扫描（PGM:531「到期预警一律提前发出……预警同时送达商户侧……
 * 宽限期内可继续经营但每日提醒」）。全平台合同关键日期与证照一轮，提醒行带商户编号，商户在商户专区可查看即送达；
 * 不发短信（BSM:1597 商户短信清单没有证照）。逻辑在 {@link ComplianceService#scanAllOn}；@Scheduled 薄包装，默认关，prod 开（每日一轮）。
 */
@Component
public class ComplianceReminderScanJob {

    private final ComplianceService service;
    private final boolean scheduledEnabled;

    public ComplianceReminderScanJob(ComplianceService service,
            @Value("${compliance.reminder.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.service = service;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "compliance.reminder.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${compliance.reminder.scheduled.cron:0 0 8 * * *}", zone = "Asia/Shanghai")
    public void scheduled() {
        if (scheduledEnabled) {
            service.scanAllOn(LocalDate.now(LocalZone.ID));
        }
    }
}
