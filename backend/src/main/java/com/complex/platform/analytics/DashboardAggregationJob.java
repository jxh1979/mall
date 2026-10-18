package com.complex.platform.analytics;
// 文件版本：V0.02
import org.springframework.beans.factory.annotation.Value;

import com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey;
import com.complex.platform.common.BusinessDayResolver;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * T12 经营看板 ETL（实现 B：@Scheduled 刷新快照）。
 *
 * <p>聚合逻辑放在可直接调用的 {@link #refreshSnapshots(LocalDate)}，@Scheduled 仅薄包装调用它——
 * 便于单测直接调方法对拍真实业务表，不依赖定时触发；定时任务通过 @ConditionalOnProperty 默认关闭，
 * 测试期不会乱跑（需显式开 analytics.dashboard-etl.scheduled.enabled=true 才注册定时刷新）。
 *
 * <p>按 r5-aggregation-metrics-draft §2 七类指标从真实业务表聚合，upsert 到
 * analytics_business_metric_snapshots（metric_key + period_type + business_date + segment_type + segment_id）。
 * 平台口径 segment_type='platform'、segment_id=NULL；商户口径 segment_type='merchant'、segment_id=merchant_id。
 * 源表无 merchant_id 的指标只产出平台口径行，商户登录读不到（按"不伪造"返回 null）。
 */
@Component
public class DashboardAggregationJob {

    // 七类 metric_code（与聚合草案 §2 对齐）。
    static final String REVENUE_TICKET = "revenue.ticket";      // 收入
    static final String REVENUE_BAR = "revenue.bar";            // 收入
    static final String REVENUE_BAR_REFUND = "revenue.bar_refund"; // 收入·酒吧商品退款（冲减 net）
    static final String REVENUE_TICKET_REFUND = "revenue.ticket_refund"; // 收入·自营售票退款（冲减 net）
    static final String REVENUE_CONTRACT_RENT = "revenue.contract_rent"; // 收入·合同租金应收（租金/物业/能源，并入 net）
    static final String REVENUE_NET = "revenue.net";            // 收入·平台净收入（派生：售票+酒吧+合同租金−退款，第三方不计）
    static final String REVENUE_PER_SQM = "revenue.per_sqm";    // 收入·坪效（派生：净收入/可经营面积；面积未配置则不产出）
    // 待确认项#1：通票售票收入的业态拆分展示口径（不并入 net，仅展示）。
    static final String REVENUE_TICKET_BAR_SHARE = "revenue.ticket_bar_share";             // 收入·通票酒吧分摊
    static final String REVENUE_TICKET_AMUSEMENT_SHARE = "revenue.ticket_amusement_share"; // 收入·通票游乐分摊
    static final String TRAFFIC_AMUSEMENT = "traffic.amusement"; // 客流
    static final String VERIFY_DONE = "verify.done";            // 核销
    static final String REDEMPTION_RATE = "verify.redemption_rate"; // 核销·券核销率（派生：核销数/发放数）
    static final String MEMBER_NEW = "member.new";              // 会员
    static final String SUPPLY_BALANCE = "supply.balance";      // 供应链
    static final String SUPPLY_LOW_STOCK = "supply.low_stock";  // 供应链·缺货预警
    static final String RISK_PENDING_REVIEW = "risk.pending_review"; // 风险
    // S2 财务应收(2026-07-02 拍板,地产 BI MVP):欠费/账龄为 as-of 时点存量(期末口径),回款为月内累计。
    static final String AR_BALANCE = "ar.balance";                   // 欠费总额(逾期未清)
    static final String AR_AGING_0_30 = "ar.aging_0_30";
    static final String AR_AGING_31_60 = "ar.aging_31_60";
    static final String AR_AGING_61_90 = "ar.aging_61_90";
    static final String AR_AGING_90P = "ar.aging_90p";               // 坏账预警区
    static final String AR_COLLECTED_MTD = "ar.collected_mtd";       // 本月实收(月初至当日)
    static final String AR_RECEIVABLE_MTD = "ar.receivable_mtd";     // 本月应收(不含未来月)
    static final String AR_COLLECTION_RATE_MTD = "ar.collection_rate_mtd"; // 回款率(分母0不产出,不伪造)
    // 出租率分母含全部商铺/摊位，不随不可租状态变化；车位、已签待交单列。
    static final String OCC_RATE = "occupancy.rate";
    static final String OCC_RENTABLE_AREA = "occupancy.rentable_area";
    static final String OCC_TOTAL_AREA = "occupancy.total_area";
    static final String OCC_LEASED_AREA = "occupancy.leased_area";
    static final String OCC_SIGNED_PENDING_AREA = "occupancy.signed_pending_area";
    static final String OCC_PARKING_RATE = "occupancy.parking_rate";
    static final String CONTRACT_EXPIRING_90D = "contract.expiring_90d";
    // V220 招商漏斗/租售比(平台口径;integration_order_syncs 无 merchant_id → rent_to_sales 仅平台级):
    static final String LEASING_LEAD_COUNT = "leasing.lead_count";                     // 招商·新增线索数(落窗)
    static final String LEASING_INTENT_CONVERSION_RATE = "leasing.intent_conversion_rate"; // 招商·线索转意向率(分母0不产出)
    static final String LEASING_SIGNED_CONVERSION_RATE = "leasing.signed_conversion_rate"; // 招商·意向转签约率(分母0不产出)
    static final String LEASING_RENT_TO_SALES = "leasing.rent_to_sales";               // 招商·租售比(合同租金/第三方营业额,分母0不产出)

    // S4 工单时效(facility 窗,未配置域回落 4 点基准):
    static final String WO_RESPONSE_HOURS_P50 = "wo.response_hours_p50";  // 响应 P50(小时,窗内无响应单不产出)
    static final String WO_HANDLE_HOURS_P50 = "wo.handle_hours_p50";      // 处理 P50(小时,窗内无完成单不产出)
    static final String WO_OVERDUE_RATE = "wo.overdue_rate";              // 超时率(分母=处理时限落窗单数,0不产出)
    static final String WO_BACKLOG = "wo.backlog";                        // 时点积压(未完成存量)
    // 清单#13:商户服务工单并入工单时效(独立键分行对照;流转无响应节点故无 mwo.response;窗同 facility)。
    static final String MWO_HANDLE_HOURS_P50 = "mwo.handle_hours_p50";    // 商户单处理 P50(提交→关单,小时)
    static final String MWO_OVERDUE_RATE = "mwo.overdue_rate";            // 商户单超时率(sla_breached+实时兜底,已取消不进分母)
    static final String MWO_BACKLOG = "mwo.backlog";                      // 商户单时点积压(不含已处理待关闭/已关闭/已取消)
    // S5 能耗(energy 窗):按表计类型分列(电/水/气/冷热单位不同,不跨类合计)。
    static final String ENERGY_TOTAL_PREFIX = "energy.total_";
    static final String ENERGY_ELECTRICITY_PER_SQM = "energy.electricity_per_sqm"; // 分母=可租面积，独立于出租率总面积；0不产出
    static final String ENERGY_ABNORMAL_COUNT = "energy.abnormal_count";

    // M1 月度滚动白名单(混合口径,2026-07-02 拍板④):流量类=月累计 SUM;存量/比率类=期末时点 LAST。
    // 月度需重算窗口的比率(P50/超时率/每平米)不做月度行——不伪造,后续需要时按月窗重算。
    public static final java.util.List<String> MONTHLY_SUM_KEYS = java.util.List.of(
            REVENUE_NET, REVENUE_TICKET, REVENUE_BAR, "revenue.contract_rent", MEMBER_NEW, "verify.done",
            "traffic.amusement", "energy.total_electricity", "energy.total_water", "energy.total_gas",
            "energy.total_heat_cold", LEASING_LEAD_COUNT);
    public static final java.util.List<String> MONTHLY_LAST_KEYS = java.util.List.of(
            AR_BALANCE, AR_AGING_90P, AR_COLLECTION_RATE_MTD, OCC_RATE, OCC_PARKING_RATE,
            OCC_SIGNED_PENDING_AREA, CONTRACT_EXPIRING_90D, WO_BACKLOG, MWO_BACKLOG);

    static final String SEGMENT_PLATFORM = "platform";
    static final String SEGMENT_MERCHANT = "merchant";
    static final String SEGMENT_PROJECT = "project";
    static final String PERIOD_DAILY = "daily";
    static final String PROJECTION_ETL = "etl_aggregate";

    /**
     * ⑤ 集团多项目 ETL：单综合体默认 project_id（与 AnalyticsService 集团总览的合成 project 标识一致）。
     * 单综合体下全部经营聚合归该综合体一个 project；多综合体启用时由真实 project 归属拆分（届时替换此默认）。
     */
    static final String DEFAULT_PROJECT_ID = "project_1";

    private final AnalyticsRepository repository;
    private final Clock clock;
    private final BusinessDayResolver businessDayResolver;
    private final boolean scheduledEnabled;

    @Autowired
    public DashboardAggregationJob(AnalyticsRepository repository, BusinessDayResolver businessDayResolver,
            @Value("${analytics.dashboard-etl.scheduled.enabled:false}") boolean scheduledEnabled) {
        this(repository, Clock.systemUTC(), businessDayResolver, scheduledEnabled);
    }

    // 测试便捷构造（默认 cutoff 经营日口径）。
    public DashboardAggregationJob(AnalyticsRepository repository) {
        this(repository, Clock.systemUTC(), new BusinessDayResolver(4), true);
    }

    DashboardAggregationJob(AnalyticsRepository repository, Clock clock) {
        this(repository, clock, new BusinessDayResolver(4), true);
    }

    DashboardAggregationJob(AnalyticsRepository repository, Clock clock, BusinessDayResolver businessDayResolver) {
        this(repository, clock, businessDayResolver, true);
    }

    DashboardAggregationJob(AnalyticsRepository repository, Clock clock, BusinessDayResolver businessDayResolver, boolean scheduledEnabled) {
        this.repository = repository;
        this.clock = clock;
        this.businessDayResolver = businessDayResolver;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 analytics.dashboard-etl.scheduled.enabled=true 时注册。 */
    @ConditionalOnProperty(name = "analytics.dashboard-etl.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${analytics.dashboard-etl.scheduled.fixed-delay-ms:300000}")
    public void scheduledRefresh() {
        if (!scheduledEnabled) {
            return;
        }
        refreshSnapshots(today());
    }

    /** 直接可调用的聚合入口：聚合 businessDate 当日七类指标并 upsert 快照。 */
    // 2d-4：整批 upsert 包一个事务，避免并发/中途失败造成快照集撕裂（平台/商户/project 行跨运行不一致）。
    // 注：scheduledRefresh 同类自调用时 @Transactional 不生效（Spring AOP self-invocation 限制），对外部触发生效。
    @Transactional
    public void refreshSnapshots(LocalDate businessDate) {
        // M3:代码指标目录同步进 analytics_metric_definitions(单一事实源投影;异常表 FK 前置)。
        Instant syncNow = Instant.now(clock);
        repository.syncMetricDefinitions(AnalyticsService.METRIC_CATALOG.stream()
                .map(entry -> new AnalyticsRecords.MetricDefinitionSyncRow(
                        entry.metricCode(), entry.domain(), entry.unit(), entry.ready(), entry.note()))
                .toList(), syncNow);

        Instant now = Instant.now(clock);

        // ⑤ 集团多项目 ETL：累积各 metric 的平台口径合计，末尾按 project_id 维度 upsert segment_type='project' 行。
        // 单综合体默认全部经营归一个 project（值=平台口径合计，口径与现状一致、不伪造多 project）。
        Map<String, BigDecimal> platformTotals = new java.util.LinkedHashMap<>();

        // 收入·售票：ticket_orders 无 merchant_id → 仅平台口径。
        upsertPlatform(REVENUE_TICKET, "finance", businessDate, repository.sumTicketRevenueCents(businessDate, "ticketing"), now, platformTotals);

        // 酒吧商品按整单付清时点、服务费扣留按原经营日；代收小费及员工应发不计收入。
        // 只汇总已确认部分，待结状态在明细列示；不含通票分摊。
        upsertByMerchant(REVENUE_BAR, "bar", businessDate, repository.sumBarRevenueCentsByMerchant(businessDate, "bar"), now, platformTotals);

        // 商品退款按真实应付足额付款时点及退款分项；服务费退款另调整原经营日扣留。
        upsertByMerchant(REVENUE_BAR_REFUND, "bar", businessDate, repository.sumBarRefundCentsByMerchant(businessDate, "bar"), now, platformTotals);

        // RC1②：收入·自营售票退款展示行 REVENUE_TICKET_REFUND 取平台退款总额（sumTicketRefundCents，平台口径）；
        //        商户维退款分量见下方 ticketRefundByMerchant(V220 直连) 并入商户行级 net。
        // 商品收退款按 platform 窗对齐；服务费扣留按其原经营日标签，不按月结运行时点。
        BigDecimal ticketRevenue = nonNull(repository.sumTicketRevenueCents(businessDate, "platform"));
        BigDecimal ticketRefund = nonNull(repository.sumTicketRefundCents(businessDate, "platform"));
        upsertPlatform(REVENUE_TICKET_REFUND, "finance", businessDate,
                nonNull(repository.sumTicketRefundCents(businessDate, "ticketing")), now, platformTotals);

        // RC1② 统一净收入 revenue.net（派生指标，财务确认口径 2026-06-18）：
        //   net = 自营售票实付(revenue.ticket) + 酒吧已确认收入(revenue.bar) − 售票退款(ticket_refund) − 酒吧退款(bar_refund)。
        //   第三方回流不计入平台净收入；退款按经营日（BusinessDayResolver）窗口冲减当日。
        //
        // 清单#4：商户行级 net 纳入售票分量（V075 merchant 归属 + V078 allocated_revenue_cent）：
        //   商户 net = 本商户售票分量(Σ allocated) + 本商户已确认商品及服务费扣留 − 本商户售票退款分摊 − 本商户酒吧退款。
        //   平台 net 保持现有口径（售票实付全额 + Σ酒吧已确认收入 − 售票退款 − Σ酒吧退款），不改用 allocated，
        //   避免 direct 出票/无 allocated 订单造成平台口径漂移。两者一致性：allocateRevenue 保证同一订单
        //   Σallocated==订单实付，故"有发券且有 allocated"的订单其商户分量 Σ 等于平台售票分量的对应部分；
        //   仅 direct 出票/历史无 allocated 的订单不进商户行（保守不归任何商户），是平台≥Σ商户的唯一差额来源——
        //   因平台与商户用各自来源各算一次，无重复计入。
        Map<String, BigDecimal> barRevenue = repository.sumBarRevenueCentsByMerchant(businessDate, "platform");
        Map<String, BigDecimal> barRefund = repository.sumBarRefundCentsByMerchant(businessDate, "platform");
        Map<String, BigDecimal> ticketAllocated = repository.sumTicketAllocatedRevenueByMerchant(businessDate, "platform");
        Map<String, BigDecimal> ticketRefundByMerchant = repository.sumTicketRefundByMerchant(businessDate, "platform");
        // P1-C 收入闭环：合同租金/物业/能源应收（ar_ap_items receivable）按 due_date 当日确认，
        // 单列 revenue.contract_rent 展示行（平台+商户），并入商户行级 net 的租金分量。
        Map<String, BigDecimal> contractRent = repository.sumContractRentReceivableByMerchant(businessDate, "platform");
        upsertByMerchant(REVENUE_CONTRACT_RENT, "finance", businessDate,
                repository.sumContractRentReceivableByMerchant(businessDate, "finance"), now, platformTotals);
        upsertNetRevenue(businessDate, ticketRevenue, ticketRefund, barRevenue, barRefund,
                ticketAllocated, ticketRefundByMerchant, contractRent, now, platformTotals);

        // 待确认项#1：通票售票收入业态拆分展示（issued_entitlements.allocated_revenue_cent 按业态/商户汇总）。
        // 仅作展示口径，不并入 revenue.net：订单实付全额已在 revenue.ticket 计入，这里只是把"售票确认收入"
        // 按酒吧/游乐业态拆出展示行，与 revenue.bar（商品及服务费扣留）口径区分，避免重复计净收入。
        upsertByMerchant(REVENUE_TICKET_BAR_SHARE, "ticketing", businessDate,
                repository.sumTicketBarShareCentsByMerchant(businessDate, "ticketing"), now, platformTotals);
        upsertByMerchant(REVENUE_TICKET_AMUSEMENT_SHARE, "ticketing", businessDate,
                repository.sumTicketAmusementShareCentsByMerchant(businessDate, "ticketing"), now, platformTotals);

        // 客流·游乐：amusement_verifications 无 merchant_id → 仅平台口径。
        upsertPlatform(TRAFFIC_AMUSEMENT, "amusement", businessDate, repository.countAmusementVerificationSuccess(businessDate), now, platformTotals);

        // 核销：ticket_verifications 有 merchant_id → 平台 + 商户口径。
        Map<String, BigDecimal> verifyDoneByMerchant = repository.countTicketVerificationSuccessByMerchant(businessDate);
        upsertByMerchant(VERIFY_DONE, "ticketing", businessDate, verifyDoneByMerchant, now, platformTotals);

        // 清单#18：券核销率（派生）= 核销数/发放数。发放数取 issued_entitlements 当日发放（同 merchant 维度）。
        // 比率指标（0~1，BigDecimal 保留 4 位小数）；分母为 0（当日无发放）按"不伪造"置 0，不产生除零。
        Map<String, BigDecimal> issuedByMerchant = repository.countTicketIssuedByMerchant(businessDate);
        upsertRedemptionRate(businessDate, verifyDoneByMerchant, issuedByMerchant, now, platformTotals);

        // 会员·新增：customers 无 merchant_id → 仅平台口径。
        upsertPlatform(MEMBER_NEW, "customer", businessDate, repository.countNewMembers(businessDate), now, platformTotals);

        // 供应链·库存余额：inventory_stock_balances 有 merchant_id → 平台 + 商户口径（存量快照，与经营日无关，落当日）。
        upsertByMerchant(SUPPLY_BALANCE, "inventory", businessDate, repository.sumInventoryBalanceByMerchant(), now, platformTotals);

        // 供应链·缺货预警：按 SKU 绝对值阈值（inventory_items.min_quantity）count(可用量<阈值)，阈值≤0 不预警（财务确认口径）。
        upsertByMerchant(SUPPLY_LOW_STOCK, "inventory", businessDate, repository.countLowStockByMerchant(), now, platformTotals);

        // 风险·待复核：supervisor_review_decisions / analytics_risk_alerts 无 merchant_id → 仅平台口径。
        upsertPlatform(RISK_PENDING_REVIEW, "governance", businessDate, repository.countPendingReview(), now, platformTotals);

        // T4.2 坪效（派生）：净收入 / 可经营面积（综合体主数据，complex_operating_params）。
        // 面积未配置/≤0 → 不产出（诚实空缺，AnalyticsService 目录注 unavailable，不伪造、不除零）。
        // 单综合体：净收入取平台口径合计，面积取 DEFAULT_PROJECT_ID；多综合体启用后按真实 project 各自面积派生。
        BigDecimal netForPerSqm = platformTotals.getOrDefault(REVENUE_NET + "|finance", BigDecimal.ZERO);
        repository.findOperatingParams(DEFAULT_PROJECT_ID)
                .map(OperatingParamsRecord::operatingAreaSqm)
                .filter(area -> area != null && area.signum() > 0)
                .ifPresent(area -> upsertPlatform(REVENUE_PER_SQM, "finance", businessDate,
                        netForPerSqm.divide(area, 2, java.math.RoundingMode.HALF_UP), now, platformTotals));

        // S2 财务应收:账龄桶(as-of=businessDate 期末存量)+欠费总额+月内回款。
        Map<String, Map<String, BigDecimal>> aging = repository.arOverdueAgingByMerchant(businessDate);
        Map<String, BigDecimal> agingTotal = new java.util.LinkedHashMap<>();
        upsertAgingBucket(AR_AGING_0_30, aging.get("a0_30"), agingTotal, businessDate, now, platformTotals);
        upsertAgingBucket(AR_AGING_31_60, aging.get("a31_60"), agingTotal, businessDate, now, platformTotals);
        upsertAgingBucket(AR_AGING_61_90, aging.get("a61_90"), agingTotal, businessDate, now, platformTotals);
        upsertAgingBucket(AR_AGING_90P, aging.get("a90p"), agingTotal, businessDate, now, platformTotals);
        // 口径硬约束:欠费总额 = Σ四桶(同一查询同一过滤,构造上桶和恒等于总额)。
        upsertByMerchant(AR_BALANCE, "finance", businessDate, agingTotal, now, platformTotals);

        LocalDate monthStart = businessDate.withDayOfMonth(1);
        Instant mtdStart = businessDayResolver.startInstant(monthStart);
        Instant mtdEnd = businessDayResolver.endInstant(businessDate);
        BigDecimal collected = nonNull(repository.arCollectedCentsBetween(mtdStart, mtdEnd));
        BigDecimal receivableMtd = nonNull(repository.arReceivableDueCentsBetween(monthStart, businessDate.plusDays(1)));
        upsertPlatform(AR_COLLECTED_MTD, "finance", businessDate, collected, now, platformTotals);
        upsertPlatform(AR_RECEIVABLE_MTD, "finance", businessDate, receivableMtd, now, platformTotals);
        if (receivableMtd.signum() > 0) {
            upsertPlatform(AR_COLLECTION_RATE_MTD, "finance", businessDate,
                    collected.divide(receivableMtd, 4, java.math.RoundingMode.HALF_UP), now, platformTotals);
        }

        // S3 出租率/去化(as-of 期末时点;比率分母 0 不产出,不伪造)。
        AnalyticsRecords.OccupancyAreas areas = repository.occupancyAreas(businessDate);
        upsertPlatform(OCC_RENTABLE_AREA, "leasing", businessDate, areas.rentableArea(), now, platformTotals);
        upsertPlatform(OCC_TOTAL_AREA, "leasing", businessDate, areas.totalArea(), now, platformTotals);
        upsertPlatform(OCC_LEASED_AREA, "leasing", businessDate, areas.leasedArea(), now, platformTotals);
        upsertPlatform(OCC_SIGNED_PENDING_AREA, "leasing", businessDate, areas.signedPendingArea(), now, platformTotals);
        if (areas.totalArea() != null && areas.totalArea().signum() > 0) {
            upsertPlatform(OCC_RATE, "leasing", businessDate,
                    areas.leasedArea().divide(areas.totalArea(), 4, java.math.RoundingMode.HALF_UP), now, platformTotals);
        }
        if (areas.parkingRentableArea() != null && areas.parkingRentableArea().signum() > 0) {
            upsertPlatform(OCC_PARKING_RATE, "leasing", businessDate,
                    areas.parkingLeasedArea().divide(areas.parkingRentableArea(), 4, java.math.RoundingMode.HALF_UP), now, platformTotals);
        }
        upsertPlatform(CONTRACT_EXPIRING_90D, "leasing", businessDate,
                BigDecimal.valueOf(repository.countExpiringContracts(businessDate, 90)), now, platformTotals);

        // S4 工单时效:P50 落窗;超时率分母=处理时限落窗的单(分母0不产出);积压=时点存量。
        AnalyticsRecords.WorkOrderSla workOrderSla = repository.workOrderSla(
                businessDayResolver.startInstant("facility", businessDate),
                businessDayResolver.endInstant("facility", businessDate), now);
        if (workOrderSla.responseP50Hours() != null) {
            upsertPlatform(WO_RESPONSE_HOURS_P50, "facility", businessDate, workOrderSla.responseP50Hours(), now, platformTotals);
        }
        if (workOrderSla.handleP50Hours() != null) {
            upsertPlatform(WO_HANDLE_HOURS_P50, "facility", businessDate, workOrderSla.handleP50Hours(), now, platformTotals);
        }
        if (workOrderSla.dueCount() > 0) {
            upsertPlatform(WO_OVERDUE_RATE, "facility", businessDate,
                    BigDecimal.valueOf(workOrderSla.overdueCount())
                            .divide(BigDecimal.valueOf(workOrderSla.dueCount()), 4, java.math.RoundingMode.HALF_UP),
                    now, platformTotals);
        }
        upsertPlatform(WO_BACKLOG, "facility", businessDate, BigDecimal.valueOf(workOrderSla.backlog()), now, platformTotals);

        // 清单#13:商户服务工单同窗并入(完成=关单;超时以 V120 sla_breached 为准+实时兜底;分母0不产出)。
        AnalyticsRecords.WorkOrderSla merchantWoSla = repository.merchantWorkOrderSla(
                businessDayResolver.startInstant("facility", businessDate),
                businessDayResolver.endInstant("facility", businessDate), now);
        if (merchantWoSla.handleP50Hours() != null) {
            upsertPlatform(MWO_HANDLE_HOURS_P50, "facility", businessDate, merchantWoSla.handleP50Hours(), now, platformTotals);
        }
        if (merchantWoSla.dueCount() > 0) {
            upsertPlatform(MWO_OVERDUE_RATE, "facility", businessDate,
                    BigDecimal.valueOf(merchantWoSla.overdueCount())
                            .divide(BigDecimal.valueOf(merchantWoSla.dueCount()), 4, java.math.RoundingMode.HALF_UP),
                    now, platformTotals);
        }
        upsertPlatform(MWO_BACKLOG, "facility", businessDate, BigDecimal.valueOf(merchantWoSla.backlog()), now, platformTotals);

        // 能耗按表计类型分列；电耗/㎡仍用可租面积（不含不可租，0不产出）。
        java.time.Instant energyStart = businessDayResolver.startInstant("energy", businessDate);
        java.time.Instant energyEnd = businessDayResolver.endInstant("energy", businessDate);
        Map<String, BigDecimal> energyByType = repository.energyConsumptionByType(energyStart, energyEnd);
        for (Map.Entry<String, BigDecimal> entry : energyByType.entrySet()) {
            upsertPlatform(ENERGY_TOTAL_PREFIX + entry.getKey(), "energy", businessDate, entry.getValue(), now, platformTotals);
        }
        BigDecimal electricity = energyByType.get("electricity");
        if (electricity != null && areas.rentableArea() != null && areas.rentableArea().signum() > 0) {
            upsertPlatform(ENERGY_ELECTRICITY_PER_SQM, "energy", businessDate,
                    electricity.divide(areas.rentableArea(), 4, java.math.RoundingMode.HALF_UP), now, platformTotals);
        }
        upsertPlatform(ENERGY_ABNORMAL_COUNT, "energy", businessDate,
                BigDecimal.valueOf(repository.countAbnormalReadings(energyStart, energyEnd)), now, platformTotals);

        // V220 招商漏斗 + 租售比(平台口径;比率分母 0 不产出,不伪造)。
        AnalyticsRecords.LeasingFunnel funnel = repository.leasingFunnel(businessDate, "leasing");
        upsertPlatform(LEASING_LEAD_COUNT, "leasing", businessDate,
                BigDecimal.valueOf(funnel.leadCount()), now, platformTotals);
        if (funnel.leadCount() > 0) {
            upsertPlatform(LEASING_INTENT_CONVERSION_RATE, "leasing", businessDate,
                    ratio(BigDecimal.valueOf(funnel.intentCount()), BigDecimal.valueOf(funnel.leadCount())),
                    now, platformTotals);
        }
        if (funnel.intentCount() > 0) {
            upsertPlatform(LEASING_SIGNED_CONVERSION_RATE, "leasing", businessDate,
                    ratio(BigDecimal.valueOf(funnel.signedCount()), BigDecimal.valueOf(funnel.intentCount())),
                    now, platformTotals);
        }
        // 租售比 = 平台合同租金应收(已聚合 revenue.contract_rent|finance) / 第三方回流营业额;分母 0 不产出。
        BigDecimal rentNumerator = platformTotals.getOrDefault(REVENUE_CONTRACT_RENT + "|finance", BigDecimal.ZERO);
        BigDecimal turnover = repository.sumThirdpartyTurnoverCents(businessDate, "leasing");
        if (turnover != null && turnover.signum() > 0) {
            upsertPlatform(LEASING_RENT_TO_SALES, "finance", businessDate,
                    ratio(rentNumerator, turnover), now, platformTotals);
        }

        // ⑤ 集团多项目 ETL：按 project_id 维度 upsert segment_type='project' 行。
        upsertByProject(businessDate, platformTotals, now);

        // M1:每次日刷后滚动更新当月 monthly 行(月末最后一刷自然定格,环比/同比随数据积累自然可比)。
        repository.rollupMonthlySnapshots(businessDate.withDayOfMonth(1), businessDate,
                MONTHLY_SUM_KEYS, MONTHLY_LAST_KEYS, now);

        // M3:日环比突变检测(2026-07-02 拍板:±surge 记可疑/±high 记高,阈值后台可调)。
        detectAnomalies(businessDate, now);

        // M4:商户经营汇总真管道(每商户一行最新态;收入与看板同源、单量=当日核销、风险=open 告警数)。
        repository.refreshMerchantSummaries(businessDate, now);
    }

    /** M3:mtd 类月初天然重置会跳变,不做环比检测。 */
    private static final java.util.Set<String> ANOMALY_EXCLUDED_KEYS = java.util.Set.of(
            AR_COLLECTED_MTD, AR_RECEIVABLE_MTD, AR_COLLECTION_RATE_MTD);

    /** M3:检测与快照同源(读回今日/昨日 platform daily 行);小基数双侧不达门槛不报(淡季防误报)。 */
    private void detectAnomalies(LocalDate businessDate, Instant now) {
        AnalyticsRecords.AnomalyConfig config = repository.readAnomalyConfig();
        if (config == null || !config.enabled()) {
            return;
        }
        // H1(2026-07-02 拍板):节假日暴涨暴跌是正常经营不是异常——经营日落在节假日区间内自动免报。
        if (repository.isWithinHoliday(businessDate)) {
            return;
        }
        Map<String, BigDecimal> today = repository.readMetricSnapshots(businessDate, null);
        Map<String, BigDecimal> previous = repository.readMetricSnapshots(businessDate.minusDays(1), null);
        for (AnalyticsService.MetricCatalogEntry entry : AnalyticsService.METRIC_CATALOG) {
            String key = entry.metricCode();
            if (!entry.ready() || ANOMALY_EXCLUDED_KEYS.contains(key)) {
                continue;
            }
            BigDecimal current = today.get(key);
            BigDecimal base = previous.get(key);
            if (current == null || base == null || base.signum() == 0) {
                continue;
            }
            BigDecimal minBase = minBaseFor(entry.unit());
            if (current.abs().compareTo(minBase) < 0 && base.abs().compareTo(minBase) < 0) {
                continue;
            }
            BigDecimal change = current.subtract(base).abs()
                    .divide(base.abs(), 4, java.math.RoundingMode.HALF_UP);
            String risk = change.compareTo(config.highPct()) >= 0 ? "high"
                    : change.compareTo(config.surgePct()) >= 0 ? "suspicious" : null;
            if (risk == null) {
                continue;
            }
            String summary = key + " 当日 " + current.stripTrailingZeros().toPlainString()
                    + " 较前一经营日 " + base.stripTrailingZeros().toPlainString()
                    + " 变动 " + change.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
            repository.insertMetricAnomaly(
                    "ano_" + key.replace('.', '_') + "_" + businessDate,
                    key, entry.domain(), risk, current, base, summary, now);
        }
    }

    private BigDecimal minBaseFor(String unit) {
        return switch (unit) {
            case "cent" -> BigDecimal.valueOf(10000);   // 双侧不足 100 元的日子不比环比
            case "rate" -> new BigDecimal("0.05");
            default -> BigDecimal.TEN;
        };
    }

    /** S2:单个账龄桶 upsert(平台+商户),并累计进欠费总额 map。 */
    private void upsertAgingBucket(
            String metricKey,
            Map<String, BigDecimal> byMerchant,
            Map<String, BigDecimal> agingTotal,
            LocalDate businessDate,
            Instant now,
            Map<String, BigDecimal> platformTotals
    ) {
        Map<String, BigDecimal> rows = byMerchant == null ? Map.of() : byMerchant;
        upsertByMerchant(metricKey, "finance", businessDate, rows, now, platformTotals);
        for (Map.Entry<String, BigDecimal> entry : rows.entrySet()) {
            agingTotal.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
        }
    }

    private void upsertPlatform(
            String metricKey,
            String domain,
            LocalDate businessDate,
            BigDecimal value,
            Instant now,
            Map<String, BigDecimal> platformTotals
    ) {
        BigDecimal normalized = value == null ? BigDecimal.ZERO : value;
        repository.upsertMetricSnapshot(
                new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_PLATFORM, null),
                normalized,
                PROJECTION_ETL,
                now
        );
        recordPlatformTotal(platformTotals, metricKey, domain, normalized);
    }

    private void upsertByMerchant(
            String metricKey,
            String domain,
            LocalDate businessDate,
            Map<String, BigDecimal> byMerchant,
            Instant now,
            Map<String, BigDecimal> platformTotals
    ) {
        BigDecimal platformTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : byMerchant.entrySet()) {
            BigDecimal value = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
            platformTotal = platformTotal.add(value);
            String merchantId = entry.getKey();
            if (merchantId != null && !merchantId.isBlank()) {
                repository.upsertMetricSnapshot(
                        new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_MERCHANT, merchantId),
                        value,
                        PROJECTION_ETL,
                        now
                );
            }
        }
        // 平台口径 = 全商户合计（含 merchant_id 为 NULL 的行也并入平台总量）。
        repository.upsertMetricSnapshot(
                new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_PLATFORM, null),
                platformTotal,
                PROJECTION_ETL,
                now
        );
        recordPlatformTotal(platformTotals, metricKey, domain, platformTotal);
    }

    /** 累积 metric 的平台口径合计（key 编码 metricKey|domain，供 project 维度复用同一 domain）。 */
    private void recordPlatformTotal(Map<String, BigDecimal> platformTotals, String metricKey, String domain, BigDecimal value) {
        platformTotals.put(metricKey + "|" + domain, value == null ? BigDecimal.ZERO : value);
    }

    /**
     * ⑤ 集团多项目 ETL：把当日平台口径各 metric 合计落到 segment_type='project' 维度。
     * 单综合体默认全部经营归 DEFAULT_PROJECT_ID 一个 project（值=平台口径合计，口径与现状一致、不伪造多 project）；
     * 多综合体启用时此处按真实 project 归属拆分（同一 metric 分组聚合到各 project_id）。
     * 与 platform/merchant 维度并存、不互相破坏（V070 唯一键含 segment_type，segment_id=project_id 独立成行）。
     */
    private void upsertByProject(LocalDate businessDate, Map<String, BigDecimal> platformTotals, Instant now) {
        for (Map.Entry<String, BigDecimal> entry : platformTotals.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            String metricKey = parts[0];
            String domain = parts.length > 1 ? parts[1] : "platform";
            repository.upsertMetricSnapshot(
                    new MetricSnapshotKey(metricKey, domain, PERIOD_DAILY, businessDate, SEGMENT_PROJECT, DEFAULT_PROJECT_ID),
                    entry.getValue(),
                    PROJECTION_ETL,
                    now
            );
        }
    }

    /**
     * 清单#18：派生并 upsert 券核销率 verify.redemption_rate = 核销数/发放数（按 merchant 行级 + 平台口径）。
     * 商户行：本商户核销数 / 本商户发放数；平台行：Σ核销数 / Σ发放数（含 merchant_id 为 NULL 的行并入平台合计）。
     * 分母为 0（当日无发放）按"不伪造"置 0，不产生除零。比率 BigDecimal 保留 4 位小数（HALF_UP）。
     */
    private void upsertRedemptionRate(
            LocalDate businessDate,
            Map<String, BigDecimal> verifyDoneByMerchant,
            Map<String, BigDecimal> issuedByMerchant,
            Instant now,
            Map<String, BigDecimal> platformTotals
    ) {
        java.util.Set<String> merchantIds = new java.util.LinkedHashSet<>();
        merchantIds.addAll(verifyDoneByMerchant.keySet());
        merchantIds.addAll(issuedByMerchant.keySet());
        BigDecimal doneTotal = BigDecimal.ZERO;
        BigDecimal issuedTotal = BigDecimal.ZERO;
        for (String merchantId : merchantIds) {
            BigDecimal done = nonNull(verifyDoneByMerchant.get(merchantId));
            BigDecimal issued = nonNull(issuedByMerchant.get(merchantId));
            doneTotal = doneTotal.add(done);
            issuedTotal = issuedTotal.add(issued);
            if (merchantId != null && !merchantId.isBlank()) {
                repository.upsertMetricSnapshot(
                        new MetricSnapshotKey(REDEMPTION_RATE, "ticketing", PERIOD_DAILY, businessDate, SEGMENT_MERCHANT, merchantId),
                        ratio(done, issued),
                        PROJECTION_ETL,
                        now
                );
            }
        }
        BigDecimal platformRate = ratio(doneTotal, issuedTotal);
        repository.upsertMetricSnapshot(
                new MetricSnapshotKey(REDEMPTION_RATE, "ticketing", PERIOD_DAILY, businessDate, SEGMENT_PLATFORM, null),
                platformRate,
                PROJECTION_ETL,
                now
        );
        recordPlatformTotal(platformTotals, REDEMPTION_RATE, "ticketing", platformRate);
    }

    /** 核销率 = 核销数/发放数；分母 ≤ 0 置 0（不伪造、不除零）。保留 4 位小数 HALF_UP。 */
    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * RC1② + 清单#4：派生并 upsert 净收入 revenue.net（按 merchant 行级 + 平台口径）。
     * 商户行：net = 本商户售票分量(Σallocated) + 本商户已确认商品及服务费扣留 − 本商户售票退款分摊 − 本商户酒吧退款。
     * 平台行：net = 售票实付 + Σ酒吧已确认收入 − 售票退款 − Σ酒吧退款（保持现有口径，不改用 allocated）。第三方回流不计入。
     */
    private void upsertNetRevenue(
            LocalDate businessDate,
            BigDecimal ticketRevenue,
            BigDecimal ticketRefund,
            Map<String, BigDecimal> barRevenueByMerchant,
            Map<String, BigDecimal> barRefundByMerchant,
            Map<String, BigDecimal> ticketAllocatedByMerchant,
            Map<String, BigDecimal> ticketRefundByMerchant,
            Map<String, BigDecimal> contractRentByMerchant,
            Instant now,
            Map<String, BigDecimal> platformTotals
    ) {
        // 商户行级 net：本商户售票分量 + 本商户已确认商品及服务费扣留 + 本商户合同租金应收 − 本商户售票退款分摊 − 本商户酒吧退款。
        java.util.Set<String> merchantIds = new java.util.LinkedHashSet<>();
        merchantIds.addAll(barRevenueByMerchant.keySet());
        merchantIds.addAll(barRefundByMerchant.keySet());
        merchantIds.addAll(ticketAllocatedByMerchant.keySet());
        merchantIds.addAll(ticketRefundByMerchant.keySet());
        merchantIds.addAll(contractRentByMerchant.keySet());
        // V221 去硬编码:各收入来源(ticket/bar/contract_rent)的计入方式由 merchant_revenue_attribution_rules 决定,
        // 而非固定 "售票分量+酒吧实收+租金−售票退款−酒吧退款"。thirdparty 目前无数据源(排除,预留)。
        // 种子=复现现有口径(ticket share_bps=10000 全额+退款净扣 / bar gross+退款净扣 / rent gross+退款忽略),故 net 值不变。
        AnalyticsRecords.MerchantRevenueAttributionRule ticketRule = repository.findDefaultAttributionRule("ticket");
        AnalyticsRecords.MerchantRevenueAttributionRule barRule = repository.findDefaultAttributionRule("bar");
        AnalyticsRecords.MerchantRevenueAttributionRule rentRule = repository.findDefaultAttributionRule("contract_rent");
        BigDecimal platformNetFromMerchants = BigDecimal.ZERO;
        for (String merchantId : merchantIds) {
            BigDecimal revenue = nonNull(barRevenueByMerchant.get(merchantId));
            BigDecimal refund = nonNull(barRefundByMerchant.get(merchantId));
            BigDecimal ticketShare = nonNull(ticketAllocatedByMerchant.get(merchantId));
            BigDecimal ticketRefundShare = nonNull(ticketRefundByMerchant.get(merchantId));
            BigDecimal rent = nonNull(contractRentByMerchant.get(merchantId));
            if (merchantId != null && !merchantId.isBlank()) {
                BigDecimal merchantNet = attributionContribution(ticketRule, ticketShare, ticketRefundShare)
                        .add(attributionContribution(barRule, revenue, refund))
                        .add(attributionContribution(rentRule, rent, BigDecimal.ZERO));
                repository.upsertMetricSnapshot(
                        new MetricSnapshotKey(REVENUE_NET, "finance", PERIOD_DAILY, businessDate, SEGMENT_MERCHANT, merchantId),
                        merchantNet,
                        PROJECTION_ETL,
                        now
                );
                platformNetFromMerchants = platformNetFromMerchants.add(merchantNet);
            }
        }
        // 2d-3（用户裁定：平台=Σ商户防漂移）：平台 net 取 Σ各商户 net，确保平台与商户口径强一致，
        // 消除"direct 出票 / 无 merchant 归属售票造成平台 ≥ Σ商户"的漂移。
        // §8B 六源含第三方：第三方回流目前尚未聚合（无独立数据源），纳入为预留（第三方=0），接入后并入各商户 net。
        BigDecimal platformNet = platformNetFromMerchants;
        repository.upsertMetricSnapshot(
                new MetricSnapshotKey(REVENUE_NET, "finance", PERIOD_DAILY, businessDate, SEGMENT_PLATFORM, null),
                platformNet,
                PROJECTION_ETL,
                now
        );
        recordPlatformTotal(platformTotals, REVENUE_NET, "finance", platformNet);
    }

    /**
     * V221:按归因口径规则计算某收入来源对商户 net 的净贡献 = 计入收入 −（若 net_deduct）计入退款。
     * 规则缺失(理论上种子已保证存在)时兜底 gross+net_deduct,即维持原全额口径,避免聚合中断。
     */
    private BigDecimal attributionContribution(
            AnalyticsRecords.MerchantRevenueAttributionRule rule, BigDecimal revenue, BigDecimal refund) {
        if (rule == null) {
            return nonNull(revenue).subtract(nonNull(refund));
        }
        BigDecimal scaledRevenue = applyAttributionMode(rule, nonNull(revenue));
        if (!"net_deduct".equals(rule.refundAttribution())) {
            return scaledRevenue;
        }
        return scaledRevenue.subtract(applyAttributionMode(rule, nonNull(refund)));
    }

    private BigDecimal applyAttributionMode(
            AnalyticsRecords.MerchantRevenueAttributionRule rule, BigDecimal amount) {
        return switch (rule.attributionMode()) {
            case "exclude" -> BigDecimal.ZERO;
            case "share_bps" -> amount.multiply(BigDecimal.valueOf(rule.shareBps()))
                    .divide(BigDecimal.valueOf(10000), 0, java.math.RoundingMode.HALF_UP);
            default -> amount; // gross
        };
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate today() {
        // 2d-4：经营日统一走 BusinessDayResolver（cutoff 切日），替换裸 UTC 自然日，避免跨日边界落错经营日。
        return businessDayResolver.businessDate(Instant.now(clock));
    }
}
