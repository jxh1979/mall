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
import org.springframework.transaction.annotation.Transactional;

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

    /** 单批重建上限（避免长事务）；缺失多时多轮扫补，幂等安全。 */
    static final int REBUILD_BATCH_LIMIT = 500;

    /** @Scheduled 薄包装:默认关闭,每日先按核销权威事实重建缺失结算(AUD-017),再算前一日对账。 */
    @ConditionalOnProperty(name = "settlement.reconciliation.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.reconciliation.scheduled.cron:0 30 3 * * *}")
    public void scheduledReconcile() {
        if (!scheduledEnabled) {
            return;
        }
        rebuildMissingSettlements(REBUILD_BATCH_LIMIT);
        reconcile(LocalDate.now(clock).minusDays(1));
    }

    /**
     * AUD-017 可审计重建：按核销权威事实(ticket_verifications success)补建缺失的核销结算增量行——
     * 消费者(AFTER_COMMIT)在提交后宕机/瞬时失败会永久缺行，本任务据权威核销事实按同一金额公式补建，
     * transaction_no {@code ST-VS-<verificationId>} 幂等（已存在则 DO NOTHING）。返回本次补建笔数(observability)。
     *
     * <p>ponytail：以"按权威事实重建"覆盖"结算行永久缺失"的资金风险（比事件重放更可靠，不依赖事件曾入队）；
     * 全量事务性 outbox + 退避/死信队列是更强的传输可靠性上限，若需要再加。
     */
    @Transactional
    public int rebuildMissingSettlements(int limit) {
        int rebuilt = 0;
        for (var f : repository.findMissingVerificationSettlementFacts(limit)) {
            boolean exclusiveByPrice = SettlementAmounts.exclusiveByPrice(
                    f.exclusiveMerchantId(), f.settlementPriceCent());
            if (!exclusiveByPrice && f.allocatedRevenueCent() == null) {
                continue; // direct 出票无快照且非专属定价：与在线消费者一致，跳过不造 net。
            }
            long verified = SettlementAmounts.incrementalVerified(
                    f.allocatedRevenueCent(), f.totalTimes(), f.k(), exclusiveByPrice, f.settlementPriceCent());
            // D6：直接把折扣总额(满额−净额)按与净额同一非专属增量斜坡分配（与在线消费者同公式，每笔>=0 且 Σ 精确，无舍入泄漏）。
            //   componentFullPrice 为空 → 0；exclusiveByPrice → 0（专属按内部价直付，无套票折扣）。
            long discountIncrement = 0L;
            if (!exclusiveByPrice && f.componentFullPriceCent() != null) {
                long discountTotal = Math.max(0L, f.componentFullPriceCent() - f.allocatedRevenueCent());
                discountIncrement = SettlementAmounts.incrementalVerified(discountTotal, f.totalTimes(), f.k(), false, null);
            }
            repository.insertImmutableFact(new SettlementTransactionRecord(
                    "st_vs_" + f.verificationId(),
                    "ST-VS-" + f.verificationId(),
                    "verification_settled",
                    f.entitlementId(),
                    f.orderId(),
                    f.ticketTypeId(),
                    f.verificationId(),
                    f.entitlementType(),
                    1,
                    f.totalTimes(),
                    f.numPeople(),
                    verified,
                    verified,
                    0L,
                    "merchant",
                    f.merchantId(),
                    null,
                    "draft",
                    f.validTo(),
                    f.exclusiveMerchantId(),
                    f.verifiedAt(),
                    discountIncrement,  // D6：重建行还原折扣（与在线一致）
                    0L, null,    // P6:重建行暂不还原综合服务费(fee=0/config=null;fee 特性未启用,重建为罕见修复路径)
                    null,        // V355 beneficiary_entity_id:重建行归商户,无主体受益方
                    null,        // S3 父票锚点:重建行 fee 恒 0(见上),不参与「一张票一道」判定,置 null
                    0L, null));  // S6:重建行暂不还原赠送权益预估成本(同 P6 fee 口径,重建为罕见修复路径)
            rebuilt++;
        }
        return rebuilt;
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
