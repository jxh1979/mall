package com.complex.platform.parking;

// 文件版本：V0.01

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * HP-P01 停车抵免成本按月入账（BSM「停车场」章「消费抵免」条「按月结算」）：业务规则在 {@link ParkingCreditService#postMonth}，
 * 这里只做 @Scheduled 薄包装（同 FacilityAlarmEscalationJob 范式，默认关闭、prod 开启，每日一轮处理已结束月份）；
 * 每个项目、承担商户、月份各自一个事务，缺钱路付账日规则等拒绝只影响该笔，下一轮重试。
 */
@Component
public class ParkingCreditCostMonthlyJob {

    private static final Logger log = LoggerFactory.getLogger(ParkingCreditCostMonthlyJob.class);

    private final ParkingCreditService credits;
    private final boolean scheduledEnabled;

    public ParkingCreditCostMonthlyJob(ParkingCreditService credits,
            @Value("${parking.credit-cost.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.credits = credits;
        this.scheduledEnabled = scheduledEnabled;
    }

    @ConditionalOnProperty(name = "parking.credit-cost.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${parking.credit-cost.scheduled.cron:0 30 4 * * *}", zone = "Asia/Shanghai")
    public void scheduledRun() {
        if (scheduledEnabled) {
            run(Instant.now());
        }
    }

    /** 返回本轮入账的组数。 */
    public int run(Instant now) {
        int posted = 0;
        for (ParkingCreditService.MonthGroup g : credits.dueMonths(now)) {
            try {
                credits.postMonth(g, now);
                posted++;
            } catch (RuntimeException e) {
                log.warn("parking credit cost posting failed: project={}, merchant={}, month={}", g.projectId(), g.merchantId(), g.costMonth(), e);
            }
        }
        return posted;
    }
}
