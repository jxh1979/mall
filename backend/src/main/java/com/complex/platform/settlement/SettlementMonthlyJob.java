package com.complex.platform.settlement;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 结算重构批2-3：月度通票分账作业（照 SettlementExpiryJob 范式，薄 @Scheduled 包装）。
 *
 * <p>业务逻辑全在 {@link TicketDistributionSettlementService#settleMonth}（可直调便于单测）；
 * 本作业仅每月初对<b>上月</b>做一次月结（默认平台口径 projectId=null）。
 * @Scheduled @ConditionalOnProperty 默认关闭——需显式开 settlement.monthly.scheduled.enabled=true 才注册。
 */
@Component
public class SettlementMonthlyJob {

    private final TicketDistributionSettlementService service;
    private final Clock clock;

    @Autowired
    public SettlementMonthlyJob(TicketDistributionSettlementService service) {
        this(service, Clock.systemUTC());
    }

    SettlementMonthlyJob(TicketDistributionSettlementService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    /** @Scheduled 薄包装:默认关闭,每月 1 日 03:00 结上月(平台口径)。 */
    @ConditionalOnProperty(name = "settlement.monthly.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.monthly.scheduled.cron:0 0 3 1 * *}")
    public void scheduledSettle() {
        YearMonth lastMonth = YearMonth.now(clock.withZone(ZoneOffset.UTC)).minusMonths(1);
        service.settleMonth(null, lastMonth);
    }
}
