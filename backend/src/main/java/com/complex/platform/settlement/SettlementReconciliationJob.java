package com.complex.platform.settlement;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 结算重构批1 第3步:双写观察期对账作业(同 DashboardAggregationJob/TicketOrderExpiryJob 模式)。
 *
 * <p>观察用途——<b>不改现有 net 聚合口径、不碰 DashboardAggregationJob</b>。仅按日比对:
 * <ul>
 *   <li>旧口径 = issued_entitlements.allocated_revenue_cent 当日聚合(direct null 跳过);</li>
 *   <li>新口径 = settlement_transactions(verification_settled)当日 verified+unverified 聚合。</li>
 * </ul>
 * verified+unverified=allocated 恒成立,故新旧总额应基本一致;差异来源=direct 出票(旧含?否——旧也跳 null)
 * 与时序(核销发生日 != 出票日,双写按核销日落 settlement.created_at,旧按出票日落 issued.created_at)。
 * variance=old−new 供财务观察,upsert settlement_reconciliation_status。
 *
 * <p>业务逻辑在可直接调用的 {@link #reconcile(LocalDate)},@Scheduled 仅薄包装(算前一日),
 * 便于单测直接调方法对拍;定时任务 @ConditionalOnProperty 默认关闭(需显式开
 * settlement.reconciliation.scheduled.enabled=true 才注册)。幂等:按 reconciliation_date upsert,可重入。
 */
@Component
public class SettlementReconciliationJob {

    private final JdbcSettlementTransactionRepository repository;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public SettlementReconciliationJob(
            JdbcSettlementTransactionRepository repository,
            @Value("${settlement.reconciliation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(repository, Clock.systemUTC(), scheduledEnabled);
    }

    SettlementReconciliationJob(JdbcSettlementTransactionRepository repository, Clock clock) {
        this(repository, clock, true);
    }

    SettlementReconciliationJob(JdbcSettlementTransactionRepository repository, Clock clock, boolean scheduledEnabled) {
        this.repository = repository;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装:默认关闭,每日算前一日。 */
    @ConditionalOnProperty(name = "settlement.reconciliation.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.reconciliation.scheduled.cron:0 30 3 * * *}")
    public void scheduledReconcile() {
        if (!scheduledEnabled) {
            return;
        }
        reconcile(LocalDate.now(clock).minusDays(1));
    }

    /**
     * 对某一日算 old/new/variance 并 upsert。日窗 [date 00:00 UTC, date+1 00:00 UTC),
     * 与 settlement_transactions.created_at DEFAULT now()(UTC)一致。
     */
    public void reconcile(LocalDate reconciliationDate) {
        Instant dayStart = reconciliationDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = reconciliationDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        long oldCent = repository.sumOldTicketAllocatedCent(dayStart, dayEnd);
        long newCent = repository.sumNewSettlementCent(dayStart, dayEnd);
        repository.upsertReconciliationStatus(reconciliationDate, oldCent, newCent, oldCent - newCent);
    }
}
