package com.complex.platform.settlement;
import org.springframework.beans.factory.annotation.Value;

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
 * <p>业务逻辑全在 {@link TicketDistributionSettlementService#settleMonth}（可直调便于单测）。
 *
 * <p>AUD-035：不再只结紧邻上月——那样一次失败/停机就永久缺月。改为每次跑回看窗口
 * [上月-backfillMonths+1, 上月] 逐月幂等补算：无单(缺月)或 draft/confirmed(未过账)月补跑，
 * 已过账(settled/reversed)月跳过（settleMonth 内部对其 fail-closed 抛 SETTLEMENT_ALREADY_POSTED，
 * 这里先按期预判状态避免撞它中断整批）。幂等：settleMonth 按项目×月 upsert 覆盖，重算安全；
 * 晚到核销也会在下次回看窗口内并入仍未过账的月。
 *
 * <p>@Scheduled @ConditionalOnProperty 默认关闭——需显式开 settlement.monthly.scheduled.enabled=true 才注册。
 */
@Component
public class SettlementMonthlyJob {

    private final TicketDistributionSettlementService service;
    private final JdbcSettlementBillRepository repository;
    private final Clock clock;
    private final boolean scheduledEnabled;

    /** 每次跑回看的月数：补齐停机/失败留下的缺月（默认 6，覆盖数月连续停机的补跑窗口）。 */
    @Value("${settlement.monthly.backfill-months:6}")
    int backfillMonths = 6;

    @Autowired
    public SettlementMonthlyJob(
            TicketDistributionSettlementService service,
            JdbcSettlementBillRepository repository,
            @Value("${settlement.monthly.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(service, repository, Clock.systemUTC(), scheduledEnabled);
    }

    SettlementMonthlyJob(TicketDistributionSettlementService service, JdbcSettlementBillRepository repository,
            Clock clock) {
        this(service, repository, clock, true);
    }

    SettlementMonthlyJob(TicketDistributionSettlementService service, JdbcSettlementBillRepository repository,
            Clock clock, boolean scheduledEnabled) {
        this.service = service;
        this.repository = repository;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装:默认关闭,每月 1 日 03:00 跑;回看窗口逐月幂等补算(AUD-035)。 */
    @ConditionalOnProperty(name = "settlement.monthly.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.monthly.scheduled.cron:0 0 3 1 * *}")
    public void scheduledSettle() {
        if (!scheduledEnabled) {
            return;
        }
        YearMonth lastMonth = YearMonth.now(clock.withZone(ZoneOffset.UTC)).minusMonths(1);
        int window = Math.max(1, backfillMonths);
        for (int i = window - 1; i >= 0; i--) {
            YearMonth month = lastMonth.minusMonths(i);
            // 平台口径 projectId=null;按期预判状态,只对缺月/未过账月补算,已过账(settled/reversed)跳过。
            String status = repository.findBillStatusByPeriod(null, month.atDay(1), month.atEndOfMonth());
            if (status == null || "draft".equals(status) || "confirmed".equals(status)) {
                service.settleMonth(null, month);
            }
        }
    }
}
