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
 * 结算重构批1 第4步:未核销权益到期作业(照 TicketOrderExpiryJob 范式,薄 @Scheduled 包装)。
 *
 * <p><b>不改任何现有过期逻辑</b>(TicketOrderExpiryJob 保持不动)——只新增本作业向 settlement_transactions
 * 写 unverified_expired 流水:每日营业后扫已过期(valid_to &lt; now)且存在未核销部分(total_times &gt; used_times)
 * 的权益,把未核销那部分金额归对应<b>经营主体</b>。
 *
 * <p><b>S8 未核销款分流(2026-07-28,业务 HTML「通票结算口径」逐字)</b>:
 * 「没有被核销的权益,那部分钱归娱乐平台公司留存;游乐通票的未核销权益款归游乐公司。」
 * 按<b>提供该权益的商户业态</b>(merchants.merchant_type,与 S11 池基过滤同一套判定源)分流:
 * 游乐业态 → amusement_ticket 流 → 游乐公司;其余 → all_pass_ticket 流 → 娱乐平台公司。
 * 主体由 {@code revenue_stream_attribution} 后台可配解析(V375 种全局默认 be_ent_platform / be_amusement),
 * 不在 Java 里写死权益类型清单;解析不到主体时<b>fail-loud</b>(有金额没有收款方=配置坏,宁可阻断不出错账)。
 * 落 beneficiary_type='entity' + beneficiary_entity_id(S1b/V355 主体列,chk_st_beneficiary 强制非空)。
 *
 * <p><b>金额切分</b>(整数分,verified+unverified=allocated 无漂移):
 * unverified = <b>四舍五入到分</b>(allocated × (total−used) / total)(R-7/D15.1,旧为整除截断向零、
 * 未核销归主体那部分恒少算);verified = allocated − unverified(取补数,两侧相加恒 = allocated,不产生尾差);
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
    private static final String BENEFICIARY_ENTITY = "entity";
    private static final String STATUS_CONFIRMED = "confirmed";
    /** S11/V348 业态词表里的游乐业态(判定源=提供方商户的 merchant_type,不是权益类型清单)。 */
    private static final String FORMAT_AMUSEMENT = "amusement";

    private final JdbcSettlementTransactionRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public SettlementExpiryJob(
            JdbcSettlementTransactionRepository repository,
            RevenueStreamAttributionRepository attributionRepository,
            @Value("${settlement.expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(repository, attributionRepository, Clock.systemUTC(), scheduledEnabled);
    }

    SettlementExpiryJob(JdbcSettlementTransactionRepository repository,
                        RevenueStreamAttributionRepository attributionRepository, Clock clock) {
        this(repository, attributionRepository, clock, true);
    }

    SettlementExpiryJob(JdbcSettlementTransactionRepository repository,
                        RevenueStreamAttributionRepository attributionRepository, Clock clock,
                        boolean scheduledEnabled) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
        this.clock = clock;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装:默认关闭,每日营业后(默认 02:30)归主体。 */
    @ConditionalOnProperty(name = "settlement.expiry.scheduled.enabled", havingValue = "true")
    @Scheduled(cron = "${settlement.expiry.scheduled.cron:0 30 2 * * *}")
    public void scheduledExpire() {
        if (!scheduledEnabled) {
            return;
        }
        expireUnverified(Instant.now(clock));
    }

    /** 扫已过期且部分未核销的权益,未核销部分按提供方业态分流归主体(幂等)。返回本次实际落账笔数(observability)。 */
    @Transactional
    public int expireUnverified(Instant now) {
        // 主体解析按流缓存(一次作业最多两条流:游乐/默认),避免逐行查配置。
        java.util.Map<String, String> entityByStream = new java.util.HashMap<>();
        int count = 0;
        for (var row : repository.findExpiredUnverifiedEntitlements(now)) {
            int total = row.totalTimes();
            int used = row.usedTimes();
            long allocated = row.allocatedRevenueCent();
            // total>used 由查询保证;total<=0 防御(理论不发生),置全额未核销。
            long unverified = total <= 0 ? allocated : MoneyRounding.prorate(allocated, total - used, total);
            unverified = Math.max(0L, Math.min(allocated, unverified));
            long verified = allocated - unverified;
            String beneficiaryEntityId = resolveBeneficiaryEntity(entityByStream, row.businessFormat());

            repository.insertImmutableFact(new SettlementTransactionRecord(
                    "st_ue_" + row.entitlementId(),
                    "ST-UE-" + row.entitlementId(),
                    TRANSACTION_TYPE_UNVERIFIED_EXPIRED,
                    row.entitlementId(),
                    row.orderId(),
                    row.ticketTypeId(),
                    null, // verification_id:归主体非单次核销
                    row.entitlementType(),
                    used,
                    total,
                    1, // num_people:批1 归主体按权益整体,不逐人拆
                    allocated,
                    verified,
                    unverified,
                    BENEFICIARY_ENTITY,
                    row.merchantId(), // 保留溯源商户(归属方是主体,merchant 仅溯源)
                    null, // project_id:批1 从 entitlement 不可得,置 NULL
                    STATUS_CONFIRMED,
                    row.validTo(),
                    null, // exclusive_merchant_id：过期未核销归主体，非专属直付，置 NULL
                    row.validTo(), // business_at：到期业务时刻=有效期截止（归主体落该月）
                    0L,  // D6 discount_amount_cent：到期归主体非核销销售，无套票折扣
                    0L, null,   // P6:到期归主体非专属核销，无综合服务费(fee=0/config=null)
                    beneficiaryEntityId, // S8:默认归娱乐平台公司/游乐业态归游乐公司(V355 主体列,V375 种子)
                    null,       // S3 父票锚点:到期归主体不是核销、fee 恒 0,不参与「一张票一道」判定,置 null
                    0L, null)); // S6:到期归主体不是核销,无赠送权益预估成本(G=0/config=null)
            count++;
        }
        return count;
    }

    /**
     * 提供方业态 → 收入流 → 主体(revenue_stream_attribution 可配,V375 种全局默认)。
     * 解析不到主体 = 归属配置被删/未迁移到位:有金额没有收款方,fail-loud 拒绝落账(宁可阻断不出错账;
     * 静默落 null 也过不了 chk_st_beneficiary 的 entity 非空 CHECK)。
     */
    private String resolveBeneficiaryEntity(java.util.Map<String, String> cache, String businessFormat) {
        String stream = FORMAT_AMUSEMENT.equals(businessFormat)
                ? "amusement_ticket"
                : TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET;
        String entityId = cache.computeIfAbsent(stream,
                s -> attributionRepository.resolveManagementEntity(null, s));
        if (entityId == null) {
            throw com.complex.platform.common.api.ApiException.conflict("REVENUE_STREAM_ATTRIBUTION_MISSING",
                    "未核销权益款无法落账:收入流「" + stream + "」未配置归属主体(V375 默认种子被删或迁移未到位),"
                            + "请先在结算配置中心配置归属并复核转正");
        }
        return entityId;
    }
}
