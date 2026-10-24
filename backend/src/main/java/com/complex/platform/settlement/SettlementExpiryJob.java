package com.complex.platform.settlement;

// 文件版本：V0.04

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
 * <p><b>S8 未核销款分流(业务 HTML §「到期未核销款」逐字)</b>:
 * 「余额按票种经营域归属:酒吧相关归娱乐平台公司,纯游乐归游乐公司,<b>演出归传媒公司</b>。」
 * 按<b>提供该权益的商户业态</b>(merchants.merchant_type,与 S11 池基过滤同一套判定源)分流:
 * 游乐业态 → amusement_ticket 流 → 游乐公司;传媒业态(V384) → performance_ticket 流 → 传媒公司;
 * 酒吧 → all_pass_ticket 流 → 娱乐平台公司;FS-023 起餐饮/零售 → 售票项目的城市项目公司,「其他」→ 商户档案指定的主管公司
 * (FS-024/V511 merchants.supervising_company_id),且一律按售票锁定的项目解析(项目特例优先)。已落到期事实的权益不再重扫。<b>业态查不到时一律 fail-loud</b>(2026-08-03),
 * 不再 COALESCE 兜底成酒吧 —— 那会把演出票款静悄悄归给娱乐平台公司。FS-024 起 fail-loud 的粒度是<b>逐商户</b>(操作员裁决 2026-10-19 第二轮第 7 条
 * 「到期作业对单家没指定的商户单行记错、不卡整批」;控制席补充约定「逐商户 try/catch,单家失败写该行错误原因、其余照常落账」):
 * 定不出收款方的商户单行记错(ERROR 日志写明商户与原因)、该商户各行不落账,其余商户照常落账、整批不停。
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
 * <p><b>幂等/可重入</b>:transaction_no='ST-UE-'(从未检票)/'ST-US-'(场次终结未核销)+entitlementId 唯一
 * (uq_st_transaction_no)→ ON CONFLICT DO NOTHING,同权益重复扫不重复归、渠道费不重复扣。project_id=售票订单锁定的项目(FS-023)。
 *
 * <p>业务逻辑在可直接调用的 {@link #expireUnverified(Instant)},@Scheduled 仅薄包装;
 * 定时任务 @ConditionalOnProperty 默认关闭(需显式开 settlement.expiry.scheduled.enabled=true 才注册)。
 */
@Component
public class SettlementExpiryJob {

    private static final String TRANSACTION_TYPE_UNVERIFIED_EXPIRED = "unverified_expired";
    private static final String BENEFICIARY_ENTITY = "entity";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SettlementExpiryJob.class);
    /** S11/V348 业态词表里的游乐业态(判定源=提供方商户的 merchant_type,不是权益类型清单)。 */
    private static final String FORMAT_AMUSEMENT = "amusement";
    /** FS-023-R2(ENUM L49「两类各自单独立项留痕、不合并」):从未完成检票环节 / 已检票场次终结未核销。 */
    static final String CATEGORY_NEVER_CHECKED_IN = "never_checked_in";
    static final String CATEGORY_SESSION_ENDED = "session_ended";

    private final JdbcSettlementTransactionRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;
    private final JdbcSettlementBillRepository billRepository;
    private final Clock clock;
    private final boolean scheduledEnabled;

    @Autowired
    public SettlementExpiryJob(
            JdbcSettlementTransactionRepository repository,
            RevenueStreamAttributionRepository attributionRepository,
            JdbcSettlementBillRepository billRepository,
            @Value("${settlement.expiry.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
        this.billRepository = billRepository;
        this.clock = Clock.systemUTC();
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

    /**
     * 扫已过期(含到期作废)且部分未核销的权益、及首场次已结束的多人票未到人份,未核销部分按（售票项目，经营域）归具体域公司(幂等)。
     * 返回本次写出的事实行数(observability,重跑时含已存在被 ON CONFLICT 跳过的行)。
     */
    @Transactional
    public int expireUnverified(Instant now) {
        java.util.Map<String, String> entityCache = new java.util.HashMap<>();
        java.util.Map<String, JdbcSettlementBillRepository.ChannelFeeConfig> feeCache = new java.util.HashMap<>();
        java.util.Map<String, String> skipped = new java.util.LinkedHashMap<>(); // FS-024:本次已单行记错的商户 → 原因
        java.util.List<Fact> facts = new java.util.ArrayList<>();
        for (var row : repository.findExpiredUnverifiedEntitlements(now)) {
            Instant businessAt = businessAt(row, now);
            if (businessAt == null) {
                continue; // 多人票首场次尚未结束、票面也未到期:不处理
            }
            int total = row.totalTimes();
            int used = row.usedTimes();
            long allocated = row.allocatedRevenueCent();
            // total>used 由查询保证;total<=0 防御(理论不发生),置全额未核销。
            long unverified = total <= 0 ? allocated : MoneyRounding.prorate(allocated, total - used, total);
            unverified = Math.max(0L, Math.min(allocated, unverified));
            // FS-023-R2:人份从未检票 → 全部未核销额属「从未完成检票环节」;已检票人份中,多次票尚未开启的剩余入场次数
            //   (DEC-R8-22「每次＝一份完整全新的票权益……未消耗的剩余入场次数跨场次保留」)同属该类,其余为场次终结未核销。
            long neverPart = unverified;
            if (row.slotCheckedIn()) {
                int visits = row.slotVisits();
                int opened = Math.min(row.slotOpened(), visits);
                neverPart = visits > opened
                        ? Math.min(unverified, MoneyRounding.prorate(allocated, visits - opened, visits)) : 0L;
            }
            long sessionPart = unverified - neverPart;
            String beneficiaryEntityId = resolveBeneficiaryEntityOrSkip(entityCache, skipped, row);
            if (beneficiaryEntityId == null) {
                continue; // FS-024:该商户定不出收款方,已单行记错,本行不落账;其余商户照常
            }
            String channel = row.salesChannel() == null || row.salesChannel().isBlank() ? null : row.salesChannel();
            var fee = feeCache.computeIfAbsent(row.projectId() + "|" + channel,
                    key -> billRepository.findChannelFee(row.projectId(), channel));
            if (!row.slotCheckedIn() || neverPart > 0) {
                facts.add(new Fact(row, CATEGORY_NEVER_CHECKED_IN, neverPart, allocated - unverified,
                        beneficiaryEntityId, businessAt, fee));
            }
            if (row.slotCheckedIn() && (sessionPart > 0 || neverPart == 0)) {
                facts.add(new Fact(row, CATEGORY_SESSION_ENDED, sessionPart, allocated - unverified,
                        beneficiaryEntityId, businessAt, fee));
            }
        }
        applyChannelFees(facts, entityCache);
        for (Fact f : facts) {
            var row = f.row;
            String prefix = CATEGORY_SESSION_ENDED.equals(f.category) ? "US" : "UE";
            repository.insertUnverifiedFact(new SettlementTransactionRecord(
                    "st_" + prefix.toLowerCase(java.util.Locale.ROOT) + "_" + row.entitlementId(),
                    "ST-" + prefix + "-" + row.entitlementId(),
                    TRANSACTION_TYPE_UNVERIFIED_EXPIRED,
                    row.entitlementId(),
                    row.orderId(),
                    row.ticketTypeId(),
                    null, // verification_id:归主体非单次核销
                    row.entitlementType(),
                    row.usedTimes(),
                    row.totalTimes(),
                    1, // num_people:归主体按权益整体,不逐人拆
                    row.allocatedRevenueCent(),
                    f.verified,
                    f.amount, // 本类未核销额(毛额);两类合计 + verified = allocated
                    BENEFICIARY_ENTITY,
                    row.merchantId(), // 保留溯源商户(归属方是主体,merchant 仅溯源)
                    row.projectId(),  // FS-023-R1:售票锁定的项目
                    STATUS_CONFIRMED,
                    row.validTo(),
                    null, // exclusive_merchant_id：过期未核销归主体，非专属直付，置 NULL
                    f.businessAt, // 票面到期=有效期截止;多人票未到人份=首场次结束时刻(FS-023-R4)
                    0L,  // D6 discount_amount_cent：到期归主体非核销销售，无套票折扣
                    0L, null,   // P6:到期归主体非专属核销，无综合服务费(fee=0/config=null)
                    f.beneficiaryEntityId,
                    null,       // S3 父票锚点:到期归主体不是核销、fee 恒 0,不参与「一张票一道」判定,置 null
                    0L, null),  // S6:到期归主体不是核销,无赠送权益预估成本;未使用票积分发行成本为零(ENUM L48)
                    new JdbcSettlementTransactionRepository.UnverifiedExtras(f.category, f.channelFee, f.fee.id(),
                            f.residual, f.residualEntityId));
        }
        return facts.size();
    }

    /**
     * FS-023-R3(ENUM L48「仅扣实际发生的通路费，经第三方渠道售出的再扣实际发生的销售渠道费……各项均只扣一次」):
     * 复用月结同一张渠道费配置(项目特例 &gt; 全局,渠道专属 &gt; 通用;官方渠道只命中通用配置)。
     * 比例模式:月结只对已核销额扣过,此处只对未核销额扣,合计即整单扣一次;
     * 固定金额模式:每张票一道——已有进池核销锚点的票月结已按首笔核销收过,不再扣;从无锚点的票在此扣一次,
     * 按各行未核销额占比分摊,尾差单列(PGM L302):单一经营域由该域公司承担,跨域组合通票由娱乐平台公司承担。
     */
    private void applyChannelFees(java.util.List<Fact> facts, java.util.Map<String, String> entityCache) {
        java.util.Map<String, java.util.List<Fact>> fixedByTicket = new java.util.LinkedHashMap<>();
        for (Fact f : facts) {
            if (f.fee.fixedAmountCent() == null) {
                f.channelFee = MoneyRounding.bpsOf(f.amount, f.fee.feeBps());
            } else if (!f.row.parentAnchored()) {
                String ticket = f.row.parentTicketId() != null ? f.row.parentTicketId() : f.row.entitlementId();
                fixedByTicket.computeIfAbsent(ticket, k -> new java.util.ArrayList<>()).add(f);
            }
        }
        for (java.util.List<Fact> group : fixedByTicket.values()) {
            long fixed = group.getFirst().fee.fixedAmountCent();
            long weight = group.stream().mapToLong(f -> f.amount).sum();
            long assigned = 0L;
            for (Fact f : group) {
                f.channelFee = weight == 0L ? 0L : MoneyRounding.prorate(fixed, f.amount, weight);
                assigned += f.channelFee;
            }
            long residual = fixed - assigned;
            if (residual != 0L) {
                Fact first = group.getFirst();
                boolean singleDomain = group.stream().map(f -> f.row.businessFormat()).distinct().count() == 1;
                first.residual = residual;
                first.residualEntityId = singleDomain ? first.beneficiaryEntityId
                        : attributedEntity(entityCache, first.row.projectId(),
                                TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET);
            }
        }
    }

    /**
     * 本行的业务时刻;未到可处理时刻返回 null。多人票本人从未检票的人份按首场次结束时刻处理
     * (ENUM L48「同票已开启的那个场次一经结束即永无法入场，于该场次结束时按本条处理，不等票面有效期届满」),其余按票面到期。
     */
    private static Instant businessAt(JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row, Instant now) {
        Instant sessionEnd = !row.slotCheckedIn() && row.parentNumPeople() > 1 && row.firstSessionKey() != null
                ? sessionEnd(row) : null;
        if (sessionEnd != null && sessionEnd.isBefore(row.validTo())) {
            return sessionEnd.isAfter(now) ? null : sessionEnd;
        }
        return row.validTo().isBefore(now) ? row.validTo() : null;
    }

    /** 首场次结束时刻（算法见 {@link com.complex.platform.ticketing.FirstSessionWindow#endOf}，门票转赠场次限制共用）。 */
    private static Instant sessionEnd(JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row) {
        return com.complex.platform.ticketing.FirstSessionWindow.endOf(row.firstSessionKey(), row.sessionDate(),
                row.sessionOpenSecond(), row.sessionCloseSecond());
    }

    /**
     * 提供方业态 → 具体域公司(按售票锁定的项目解析)。口径逐字出自 ENUM L48/L49:
     * 「酒吧相关归娱乐平台公司，纯游乐归游乐公司，演出归传媒公司，餐饮与零售归城市项目公司」;
     * ENUM L195「选“其他”的商户，必须在商户档案里指定一家主管公司……未指定的不得开通任何结算与分账」。
     * 酒吧/游乐/传媒经 {@code revenue_stream_attribution} 按(项目,收入流)解析,项目特例优先于平台默认。
     *
     * <p><b>定不出收款方一律阻断(fail-loud)</b>:业态缺失、订单没有锁定项目、项目城市项目公司不是恰好一家、
     * 「其他」商户未指定有效主管公司、归属未配置——有金额没有收款方,宁可停下来补档案,不默认归娱乐平台公司。
     */
    private String resolveBeneficiaryEntity(java.util.Map<String, String> cache,
                                            JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row) {
        String businessFormat = row.businessFormat();
        if (businessFormat == null) {
            throw com.complex.platform.common.api.ApiException.conflict("MERCHANT_BUSINESS_FORMAT_MISSING",
                    "未核销权益款无法落账:权益「" + row.entitlementId() + "」查不到提供方商户的经营业态"
                            + "(商户:" + (row.merchantId() == null ? "未指定" : row.merchantId()) + ")。"
                            + "到期余额要按票种经营域归属(酒吧相关归娱乐平台公司、纯游乐归游乐公司、演出归传媒公司、餐饮与零售归城市项目公司),"
                            + "业态缺失就定不出这笔钱该归谁;请先在商户档案补齐业态后重跑,"
                            + "不得默认按酒吧归娱乐平台公司");
        }
        String project = row.projectId();
        if (project == null) {
            throw com.complex.platform.common.api.ApiException.conflict("TICKET_ORDER_PROJECT_MISSING",
                    "未核销权益款无法落账:权益「" + row.entitlementId() + "」的售票订单没有锁定综合体项目,"
                            + "按(项目,经营域)定不出具体域公司;不按当前操作者或商户反推");
        }
        return switch (businessFormat) {
            case "catering", "retail" -> cache.computeIfAbsent("city|" + project, k -> cityProjectCompany(project));
            case "other" -> supervisingCompany(row);
            case FORMAT_AMUSEMENT -> attributedEntity(cache, project, "amusement_ticket");
            case TicketDistributionSettlementService.FORMAT_MEDIA -> attributedEntity(cache, project, "performance_ticket");
            default -> attributedEntity(cache, project, TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET);
        };
    }

    /**
     * FS-024(操作员裁决 2026-10-19 第二轮第 7 条;控制席补充约定):逐商户 try/catch —— 定不出收款方(业态缺失、订单无项目、
     * 城市项目公司不是恰好一家、「其他」未指定主管公司、归属未配置)的商户,单行记错(ERROR 日志写明商户与原因)并返回 null,
     * 该商户本次后续各行不再重复记错;其余商户照常落账,整批不停。
     */
    private String resolveBeneficiaryEntityOrSkip(java.util.Map<String, String> cache, java.util.Map<String, String> skipped,
                                                  JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row) {
        String merchantKey = row.merchantId() == null ? "(无提供方商户)" : row.merchantId();
        if (skipped.containsKey(merchantKey)) {
            return null;
        }
        try {
            return resolveBeneficiaryEntity(cache, row);
        } catch (com.complex.platform.common.api.ApiException blocked) {
            skipped.put(merchantKey, blocked.getMessage());
            log.error("到期未核销款落账跳过商户「{}」(本次作业该商户各行均不落账,其余商户照常):{}", merchantKey, blocked.getMessage());
            return null;
        }
    }

    private String attributedEntity(java.util.Map<String, String> cache, String project, String stream) {
        String entityId = cache.computeIfAbsent(project + "|" + stream,
                k -> attributionRepository.resolveManagementEntity(project, stream));
        if (entityId == null) {
            throw com.complex.platform.common.api.ApiException.conflict("REVENUE_STREAM_ATTRIBUTION_MISSING",
                    "未核销权益款无法落账:收入流「" + stream + "」未配置归属主体(V375/V384 默认种子被删或迁移未到位),"
                            + "请先在结算配置中心配置归属并复核转正");
        }
        return entityId;
    }

    private String cityProjectCompany(String project) {
        java.util.List<String> companies = repository.findProjectCompanies(project);
        if (companies.size() != 1) {
            throw com.complex.platform.common.api.ApiException.conflict("CITY_PROJECT_COMPANY_MISSING",
                    "未核销权益款无法落账:餐饮与零售份额归该项目城市项目公司,项目「" + project + "」有效城市项目公司"
                            + companies.size() + "家,须恰好一家;请先在主体档案关联后重跑");
        }
        return companies.getFirst();
    }

    /**
     * ENUM L195:「其他」业态的主管公司须在商户档案里指定并说明理由(FS-024/V511 merchants.supervising_company_id,裁决
     * 「到期归属按该主管公司」);未指定一律阻断(经逐商户 try/catch 转为该商户单行记错),不回落到商户自身法律主体
     * (merchants.business_entity_id 不是主管公司)。
     */
    private String supervisingCompany(JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row) {
        if (row.supervisingCompanyId() == null) {
            throw com.complex.platform.common.api.ApiException.conflict("MERCHANT_SUPERVISING_COMPANY_MISSING",
                    "未核销权益款无法落账:业态为「其他」的商户「" + row.merchantId() + "」未在商户档案指定主管公司,"
                            + "未指定的不得开通任何结算与分账;请先补齐商户档案后重跑");
        }
        return row.supervisingCompanyId();
    }

    /** 一条待写的到期未核销事实(一权益一类一行)。渠道费在全部行收齐后按票分摊。 */
    private static final class Fact {
        final JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row;
        final String category;
        final long amount;
        final long verified;
        final String beneficiaryEntityId;
        final Instant businessAt;
        final JdbcSettlementBillRepository.ChannelFeeConfig fee;
        long channelFee;
        long residual;
        String residualEntityId;

        Fact(JdbcSettlementTransactionRepository.ExpiredUnverifiedRow row, String category, long amount, long verified,
             String beneficiaryEntityId, Instant businessAt, JdbcSettlementBillRepository.ChannelFeeConfig fee) {
            this.row = row;
            this.category = category;
            this.amount = amount;
            this.verified = verified;
            this.beneficiaryEntityId = beneficiaryEntityId;
            this.businessAt = businessAt;
            this.fee = fee;
        }
    }
}
