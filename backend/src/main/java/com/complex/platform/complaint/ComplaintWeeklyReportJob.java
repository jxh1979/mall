package com.complex.platform.complaint;

// 文件版本：V0.01

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 M 段第 111 条 111-R4 客诉周报定时作业（BSM:1385「系统每周自动生成客诉周报」）。同 FacilityWorkOrderEscalationJob 范式：业务规则在
 * {@link ComplaintWeeklyReportService#sendWeeklyReports}，这里只做 @Scheduled 薄包装，默认关闭，prod 开启（北京时间每周一 08:00 发上一周的周报）。
 */
@Component
public class ComplaintWeeklyReportJob {

    private final ComplaintWeeklyReportService service;
    private final boolean scheduledEnabled;

    public ComplaintWeeklyReportJob(ComplaintWeeklyReportService service,
            @Value("${complaint.weekly-report.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.service = service;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "complaint.weekly-report.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${complaint.weekly-report.scheduled.cron:0 0 8 * * MON}", zone = "Asia/Shanghai")
    public void scheduled() {
        if (scheduledEnabled) {
            service.sendWeeklyReports(Instant.now());
        }
    }
}
