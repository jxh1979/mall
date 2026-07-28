package com.complex.platform.settlement;
import org.springframework.beans.factory.annotation.Value;

import com.complex.platform.common.money.MoneyRounding;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批1 第4步:未核销权益到期归项目作业(照 TicketOrderExpiryJob 范式,薄 @Scheduled 包装)。
 *
 * <p><b>不改任何现有过期逻辑</b>(TicketOrderExpiryJob 保持不动)——只新增本作业向 settlement_transactions
 * 写 unverified_expired 流水:每日营业后扫已过期(valid_to &lt; now)且存在未核销部分(total_times &gt; used_times)
 * 的权益,把未核销那部分金额直接归项目公司(beneficiary_type=project,settlement_status=confirmed 直接确认)。
 *
 * <p><b>金额切分</b>(整数分,verified+unverified=allocated 无漂移):
 * unverified = <b>四舍五入到分</b>(allocated × (total−used) / total)(R-7/D15.1,旧为整除截断向零、
 * 未核销归项目那部分恒少算);verified = allocated − unverified(取补数,两侧相加恒 = allocated,不产生尾差);
 * verified_count = used。
 * allocated==null(direct 出票无分摊快照)→ 仓库查询已 WHERE allocated IS NOT NULL 排除,不会到这。
 *
 * <p><b>幂等/可重入</b>:transaction_no='ST-UE-'+entitlementId 唯一(uq_st_transaction_no)→
 * ON CONFLICT DO NOTHING,同权益重复扫不重复归。project_id 批1 从 entitlement 不可得,置 NULL。
 *
 * <p>业务逻辑在可直接调用的 {@link #expireUnverified(Instant)},@Scheduled 仅薄包装;
 * 定时任务 @ConditionalOnProperty 默认关闭(需显式开 settlement.expiry.scheduled.enabled=true 才注册)。
 */
@Component
public class SettlementExpiryJob {

    private static final String TRANSACTION_TYPE_UNVERIFIED_EXPIRED = "unverified_expired";
    private static final String BENEFICIARY_PROJECT = "project";
    private static final String STATUS_CONFIRMED = "confirmed";

    private final JdbcSettlementTransactionRepository repository;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public SettlementExpiryJob(
            JdbcSettlementTransactionRepository repository,
            @Value("${settlement.expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(repository, Clock.systemUTC(), scheduledEnabled);
    }

    SettlementExpiryJob(JdbcSettlementTransactionRepository repository, Clock clock) {
        this(repository, clock, true);
    }

    SettlementExpiryJob(JdbcSettlementTransactionRepository repository, Clock clock, boolean scheduledEnabled) {
        this.repository = repository;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装:默认关闭,每日营业后(默认 02:30)归项目。 */
    @ConditionalOnProperty(name = "settlement.expiry.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.expiry.scheduled.cron:0 30 2 * * *}")
    public void scheduledExpire() {
        if (!scheduledEnabled) {
            return;
        }
        expireUnverified(Instant.now(clock));
    }

    /** 扫已过期且部分未核销的权益,未核销部分归项目(幂等)。返回本次实际归项目笔数(observability)。 */
    @Transactional
    public int expireUnverified(Instant now) {
        int count = 0;
        for (var row : repository.findExpiredUnverifiedEntitlements(now)) {
            int total = row.totalTimes();
            int used = row.usedTimes();
            long allocated = row.allocatedRevenueCent();
            // total>used 由查询保证;total<=0 防御(理论不发生),置全额未核销。
            long unverified = total <= 0 ? allocated : MoneyRounding.prorate(allocated, total - used, total);
            unverified = Math.max(0L, Math.min(allocated, unverified));
            long verified = allocated - unverified;

            repository.insertImmutableFact(new SettlementTransactionRecord(
                    "st_ue_" + row.entitlementId(),
                    "ST-UE-" + row.entitlementId(),
                    TRANSACTION_TYPE_UNVERIFIED_EXPIRED,
                    row.entitlementId(),
                    row.orderId(),
                    row.ticketTypeId(),
                    null, // verification_id:归项目非单次核销
                    row.entitlementType(),
                    used,
                    total,
                    1, // num_people:批1 归项目按权益整体,不逐人拆
                    allocated,
                    verified,
                    unverified,
                    BENEFICIARY_PROJECT,
                    row.merchantId(), // 保留溯源商户(归属方是 project,merchant 仅溯源)
                    null, // project_id:批1 从 entitlement 不可得,置 NULL
                    STATUS_CONFIRMED,
                    row.validTo(),
                    null, // exclusive_merchant_id：过期未核销归项目，非专属直付，置 NULL
                    row.validTo(), // business_at：到期业务时刻=有效期截止（归项目落该月）
                    0L,  // D6 discount_amount_cent：到期归项目非核销销售，无套票折扣
                    0L, null)); // P6:到期归项目非专属核销，无综合服务费(fee=0/config=null)
            count++;
        }
        return count;
    }
}
