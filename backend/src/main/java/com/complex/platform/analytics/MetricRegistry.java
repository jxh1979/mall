package com.complex.platform.analytics;

import com.complex.platform.common.api.ApiException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KPI 指标登记簿（2026-07-18 拍板方案A：代码内静态注册，非数据库）。
 * 每个指标一条登记 = 中文名 / 口径说明 / 来源查询（经 AnalyticsService.metricDetail 按 metricKey 分派到
 * AnalyticsRepository 的同源明细查询）/ 明细列定义 / 权限码 / 支持的范围维度 / 数值类型。
 *
 * 统一明细端点 GET /api/v1/analytics/metric-detail 按登记项二次校验权限，返回
 * 「分页明细行 + 合计校验值 + 口径说明 + 数据截至时间」，保证「页面数字 = 明细合计」同源。
 *
 * 口径铁律：可加(additive)=区间求和；比率(ratio)=分子/分母（明细分两页签）；余额(balance)=区间末值。
 * 首批 + 第二批（酒吧域）登记均为平台口径，暂不支持商户/项目维度过滤（登记 supportsMerchantDim=false）。
 */
public final class MetricRegistry {

    /** 数值类型：口径铁律三类（对外 JSON 输出小写字符串）。 */
    public static final String TYPE_ADDITIVE = "additive";
    public static final String TYPE_RATIO = "ratio";
    public static final String TYPE_BALANCE = "balance";

    /** 明细列定义：key=明细行 map 内键（camelCase），label=表头中文。 */
    public record MetricColumn(String key, String label) {
    }

    /**
     * 指标登记项。
     * windowDomain：日期过滤用经营日窗域（V201 可配日切）；rangeFilterOptional=true 表示不传日期默认全历史
     * （如对账差异笔数，与来源页「全部差异」合计一致），false 表示缺省取当前经营日。
     * traceLink*：行级单据下钻（TraceLink 范式）；比率类仅分子页签有行级下钻。
     */
    public record MetricDefinition(
            String metricKey,
            String name,
            String valueType,
            String unit,
            String caliber,
            String source,
            String permission,
            String windowDomain,
            boolean rangeFilterOptional,
            boolean supportsMerchantDim,
            String sourcePageId,
            String traceLinkPageId,
            String traceLinkColumnKey,
            List<MetricColumn> columns,
            List<MetricColumn> denominatorColumns
    ) {
    }

    private static final Map<String, MetricDefinition> REGISTRY = buildRegistry();

    private static Map<String, MetricDefinition> buildRegistry() {
        Map<String, MetricDefinition> registry = new LinkedHashMap<>();
        register(registry, new MetricDefinition(
                "revenue.ticket",
                "票务收入",
                TYPE_ADDITIVE,
                "分",
                "已支付票务订单实付金额合计（分）。可加口径=区间求和：按下单时间落票务经营日窗、仅已支付订单；"
                        + "与总控驾驶舱「票务收入」卡及看板 ETL（revenue.ticket）同一谓词同源。",
                "票务订单台账（实时查询，数据截至=查询时刻；看板快照为 ETL 跑批时点，可能存在时滞）。",
                "analytics.dashboard.view",
                "ticketing",
                false,
                false,
                "ADM-OV-001",
                "ADM-TKT-003",
                "id",
                List.of(
                        new MetricColumn("orderNo", "订单号"),
                        new MetricColumn("quantity", "数量"),
                        new MetricColumn("salesChannel", "销售渠道"),
                        new MetricColumn("amountCent", "实付金额（分）"),
                        new MetricColumn("createdAt", "下单时间"),
                        new MetricColumn("paidAt", "支付时间")),
                null));
        register(registry, new MetricDefinition(
                "finance.recon_diff_count",
                "对账差异笔数",
                TYPE_ADDITIVE,
                "笔",
                "对账差异逐笔计数（含已关闭，与对账差异页「对账差异」数一致）。可加口径=区间求和：按差异发现时间"
                        + "落财务经营日窗计数；不传日期=全部历史。",
                "对账差异台账（实时查询）。",
                "finance.reconciliation.view",
                "finance",
                true,
                false,
                "ADM-FIN-002",
                "ADM-FIN-002",
                "id",
                List.of(
                        new MetricColumn("id", "差异号"),
                        new MetricColumn("differenceType", "差异类型"),
                        new MetricColumn("amountCent", "差异金额（分）"),
                        new MetricColumn("source", "来源"),
                        new MetricColumn("status", "状态"),
                        new MetricColumn("createdAt", "发现时间")),
                null));
        register(registry, new MetricDefinition(
                "energy.total_electricity",
                "用电量",
                TYPE_ADDITIVE,
                "kWh",
                "电表读数用量合计（kWh）。可加口径=区间求和：按抄表时间落能耗经营日窗、仅电表；"
                        + "与能耗分析页「用电」卡及看板 ETL（energy.total_electricity）同一谓词同源。",
                "能耗抄表台账（电表读数，实时查询）。",
                "energy.reading.manage",
                "energy",
                false,
                false,
                "ADM-ANA-007",
                "ADM-ENE-001",
                "id",
                electricityColumns(),
                null));
        register(registry, new MetricDefinition(
                "energy.electricity_per_sqm",
                "每平米电耗",
                TYPE_RATIO,
                "kWh/㎡",
                "比率口径=分子/分母，明细分「分子明细/分母明细」两页签。分子=区间用电量合计（kWh，同「用电量」口径）；"
                        + "分母=可租面积（㎡，商铺/摊位且未停用空间面积合计，与出租率同分母）。"
                        + "注意：分母按空间台账当前状态计，历史区间查询时分母为当前值；分母为 0 不产出（不伪造）。",
                "分子=能耗抄表台账（电表读数）；分母=空间台账（实时查询）。",
                "energy.reading.manage",
                "energy",
                false,
                false,
                "ADM-ANA-007",
                "ADM-ENE-001",
                "id",
                electricityColumns(),
                List.of(
                        new MetricColumn("spaceName", "空间"),
                        new MetricColumn("spaceType", "类型"),
                        new MetricColumn("floorNo", "楼层"),
                        new MetricColumn("areaSquareMeter", "面积（㎡）"))));
        register(registry, new MetricDefinition(
                "finance.ar_balance",
                "应收欠费余额",
                TYPE_BALANCE,
                "分",
                "逾期未清应收余额=Σ未收金额（分）（应收方向、未关闭、未收金额>0、到期日早于截至日）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前财务经营日）为截至日，起始日期不参与；"
                        + "与看板 ETL（ar.balance，四桶账龄合计）同一谓词同源。注意：明细按台账当前未收金额计，历史截至日仅影响到期日截止。",
                "应收应付台账（实时查询）。",
                "finance.ar_ap.view",
                "finance",
                false,
                false,
                "ADM-OV-001",
                "ADM-FIN-001",
                "id",
                List.of(
                        new MetricColumn("id", "单据号"),
                        new MetricColumn("merchantId", "商户"),
                        new MetricColumn("sourceType", "费用类型"),
                        new MetricColumn("dueDate", "到期日"),
                        new MetricColumn("unpaidAmountCent", "未收金额（分）"),
                        new MetricColumn("status", "状态")),
                null));

        // ==== 第二批（酒吧域，2026-07-18）：谓词逐字抄各页面现数字的既有取数（不自造口径）====
        register(registry, new MetricDefinition(
                "revenue.bar",
                "酒吧收入",
                TYPE_ADDITIVE,
                "分",
                "已结算账单实收合计（分）。可加口径=区间求和：按结算时间落酒吧经营日窗、剔除已冲正（reversed）流水；"
                        + "与总控驾驶舱「酒吧收入」卡及看板 ETL（revenue.bar）同一谓词同源；仅现场消费实收，不含通票售票分摊。",
                "酒吧账单结算流水台账（bar_bill_settlements，实时查询；看板快照为 ETL 跑批时点，可能存在时滞）。",
                "analytics.dashboard.view",
                "bar",
                false,
                false,
                "ADM-OV-001",
                "ADM-BAR-002",
                "billId",
                List.of(
                        new MetricColumn("id", "结算流水号"),
                        new MetricColumn("billId", "账单号"),
                        new MetricColumn("merchantId", "商户"),
                        new MetricColumn("settleChannel", "结算渠道"),
                        new MetricColumn("settlementAmountCent", "实收金额（分）"),
                        new MetricColumn("settledAt", "结算时间")),
                null));
        register(registry, new MetricDefinition(
                "bar.order_count",
                "酒吧订单数",
                TYPE_ADDITIVE,
                "笔",
                "酒吧订单逐笔计数（含全部状态，与订单/出品/账单页「订单数」及销售提成页「订单数」一致）。"
                        + "可加口径=区间求和：按下单时间落酒吧经营日窗计数；不传日期=全部历史（与页面全量列表一致）。",
                "酒吧订单台账（bar_orders，实时查询）。",
                "bar.order.view",
                "bar",
                true,
                false,
                "ADM-BAR-002",
                "ADM-BAR-002",
                "id",
                barOrderColumns(),
                null));
        register(registry, new MetricDefinition(
                "bar.order_amount",
                "酒吧订单金额",
                TYPE_ADDITIVE,
                "分",
                "酒吧订单金额合计（分，含全部状态订单，与订单/出品/账单页「订单金额」卡一致；下单口径非实收口径，"
                        + "实收见「酒吧收入」）。可加口径=区间求和：按下单时间落酒吧经营日窗；不传日期=全部历史。",
                "酒吧订单台账（bar_orders，实时查询）。",
                "bar.order.view",
                "bar",
                true,
                false,
                "ADM-BAR-002",
                "ADM-BAR-002",
                "id",
                barOrderColumns(),
                null));
        register(registry, new MetricDefinition(
                "bar.open_order_count",
                "未完结酒吧订单",
                TYPE_BALANCE,
                "笔",
                "当前未完结订单数=状态非「已支付/已取消」的酒吧订单计数（与订单/出品/账单页「未完结」卡一致）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前酒吧经营日）为截至日，起始日期不参与；"
                        + "仅限制下单时间早于截至日窗末。注意：状态按台账当前值计，历史截至日仅影响下单时间截止。",
                "酒吧订单台账（bar_orders，实时查询）。",
                "bar.order.view",
                "bar",
                false,
                false,
                "ADM-BAR-002",
                "ADM-BAR-002",
                "id",
                barOrderColumns(),
                null));
        register(registry, new MetricDefinition(
                "bar.saved_bottle_count",
                "存酒登记总数",
                TYPE_ADDITIVE,
                "笔",
                "存酒登记逐笔计数（含在存/已领取/已取消/已过期全部状态，与存酒页「存酒总数」一致）。"
                        + "可加口径=区间求和：按存入时间落酒吧经营日窗计数；不传日期=全部历史。",
                "存酒台账（saved_bottles，实时查询）。",
                "bar.saved_bottle.manage",
                "bar",
                true,
                false,
                "ADM-BAR-003",
                "ADM-BAR-003",
                "id",
                savedBottleColumns(),
                null));
        register(registry, new MetricDefinition(
                "bar.saved_bottle_stored",
                "在存存酒",
                TYPE_BALANCE,
                "瓶",
                "当前在存（状态=在存）存酒笔数（与存酒页「在存」卡一致）。余额口径=区间末值："
                        + "取结束日期（缺省=当前酒吧经营日）为截至日，起始日期不参与；仅限制存入时间早于截至日窗末。"
                        + "注意：状态按台账当前值计，历史截至日仅影响存入时间截止。",
                "存酒台账（saved_bottles，实时查询）。",
                "bar.saved_bottle.manage",
                "bar",
                false,
                false,
                "ADM-BAR-003",
                "ADM-BAR-003",
                "id",
                savedBottleColumns(),
                null));
        register(registry, new MetricDefinition(
                "bar.saved_bottle_quantity",
                "存酒剩余数量",
                TYPE_BALANCE,
                "数量",
                "Σ剩余数量=全部存酒登记行的当前剩余数量直加（含已领完=0行，与存酒页「剩余数量」口径一致）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前酒吧经营日）为截至日，起始日期不参与；"
                        + "仅限制存入时间早于截至日窗末，剩余数量按台账当前值计。"
                        + "注意：各登记行计量单位可能不同（瓶/支等），合计为数量直加。",
                "存酒台账（saved_bottles，实时查询）。",
                "bar.saved_bottle.manage",
                "bar",
                false,
                false,
                "ADM-BAR-003",
                "ADM-BAR-003",
                "id",
                savedBottleColumns(),
                null));

        // ==== 第三批（票务域，2026-07-19）：谓词逐字抄各页面现数字的既有取数（不自造口径）====
        // 售票域（ADM-TKT-003 出票与核销追踪）：数字来自 GET /ticketing/orders 平台口径全量列表。
        register(registry, new MetricDefinition(
                "ticket.order_count",
                "售票订单数",
                TYPE_ADDITIVE,
                "笔",
                "票务订单逐笔计数（含全部状态，与出票/核销追踪页「订单」卡一致）。可加口径=区间求和："
                        + "按下单时间落票务经营日窗计数；不传日期=全部历史（与页面 GET /ticketing/orders 平台口径全量列表一致）。",
                "票务订单台账（ticket_orders，实时查询）。",
                "ticket.order.read",
                "ticketing",
                true,
                false,
                "ADM-TKT-003",
                "ADM-TKT-003",
                "id",
                ticketOrderColumns(),
                null));
        register(registry, new MetricDefinition(
                "ticket.paid_order_count",
                "已支付订单数",
                TYPE_ADDITIVE,
                "笔",
                "已支付票务订单逐笔计数（pay_status='paid'，与出票/核销追踪页「已支付」卡一致；支付态按台账当前值）。"
                        + "可加口径=区间求和：按下单时间落票务经营日窗计数；不传日期=全部历史。",
                "票务订单台账（ticket_orders，实时查询）。",
                "ticket.order.read",
                "ticketing",
                true,
                false,
                "ADM-TKT-003",
                "ADM-TKT-003",
                "id",
                ticketOrderColumns(),
                null));
        register(registry, new MetricDefinition(
                "ticket.pending_issue_count",
                "待出票订单数",
                TYPE_BALANCE,
                "笔",
                "当前待出票订单数=出票状态非「已出票」（issue_status<>'issued'）的票务订单计数（与出票/核销追踪页「待出票」卡一致）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前票务经营日）为截至日，起始日期不参与；仅限制下单时间早于截至日窗末。"
                        + "注意：出票状态按台账当前值计，历史截至日仅影响下单时间截止。",
                "票务订单台账（ticket_orders，实时查询）。",
                "ticket.order.read",
                "ticketing",
                false,
                false,
                "ADM-TKT-003",
                "ADM-TKT-003",
                "id",
                ticketOrderColumns(),
                null));

        // 游乐场次域（ADM-AMU-001 项目与场次）：数字来自 GET /amusement/sessions 平台口径全量列表。
        register(registry, new MetricDefinition(
                "amusement.session_count",
                "游乐场次数",
                TYPE_ADDITIVE,
                "场",
                "游乐场次逐条计数（含全部状态，与项目与场次页「场次」卡一致）。可加口径=区间求和："
                        + "按创建时间落游乐经营日窗计数；不传日期=全部历史（与页面 GET /amusement/sessions 平台口径全量列表一致）。",
                "游乐场次台账（amusement_sessions，实时查询）。",
                "amusement.session.view",
                "amusement",
                true,
                false,
                "ADM-AMU-001",
                "ADM-AMU-001",
                "id",
                sessionColumns(),
                null));
        register(registry, new MetricDefinition(
                "amusement.session_capacity",
                "游乐场次总容量",
                TYPE_ADDITIVE,
                "人次",
                "Σ场次容量=全部游乐场次 capacity 直加（与项目与场次页「总容量」卡一致）。可加口径=区间求和："
                        + "按创建时间落游乐经营日窗；不传日期=全部历史。容量为建场即定，随场次集合直加。",
                "游乐场次台账（amusement_sessions，实时查询）。",
                "amusement.session.view",
                "amusement",
                true,
                false,
                "ADM-AMU-001",
                "ADM-AMU-001",
                "id",
                sessionColumns(),
                null));

        // 团体/包场域（ADM-AMU-002 团体/包场）：数字来自 GET /amusement/group-bookings 平台口径列表。
        register(registry, new MetricDefinition(
                "amusement.group_booking_count",
                "团体预约数",
                TYPE_ADDITIVE,
                "笔",
                "团体/包场预约逐笔计数（含全部状态，与团体/包场页「团体预约」卡一致）。可加口径=区间求和："
                        + "按下单时间落游乐经营日窗计数；不传日期=全部历史。注意：页面列表默认取首页（size=200），"
                        + "本合计为全量真实计数，超首页时本合计更完整。",
                "团体/包场预约台账（amusement_group_bookings，实时查询）。",
                "amusement.group_booking.view",
                "amusement",
                true,
                false,
                "ADM-AMU-002",
                "ADM-AMU-002",
                "id",
                groupBookingColumns(),
                null));
        register(registry, new MetricDefinition(
                "amusement.group_reserved_capacity",
                "团体占用容量",
                TYPE_ADDITIVE,
                "人次",
                "Σ占用容量=全部团体/包场预约 reserved_capacity 直加（与团体/包场页「容量占用」卡一致）。"
                        + "可加口径=区间求和：按下单时间落游乐经营日窗；不传日期=全部历史。",
                "团体/包场预约台账（amusement_group_bookings，实时查询）。",
                "amusement.group_booking.view",
                "amusement",
                true,
                false,
                "ADM-AMU-002",
                "ADM-AMU-002",
                "id",
                groupBookingColumns(),
                null));

        // 游乐核销域（ADM-AMU-005 游乐核销记录）：数字来自 GET /amusement/verifications 平台口径列表（默认无筛选=全量）。
        register(registry, new MetricDefinition(
                "amusement.entry_count",
                "游乐入场核销数",
                TYPE_ADDITIVE,
                "次",
                "入场方向核销逐条计数（direction='entry' 或空，含成功/失败全部结果，与游乐核销记录页「入场」卡一致）。"
                        + "可加口径=区间求和：按核销时间落游乐经营日窗计数；不传日期=全部历史（与页面默认无筛选全量列表一致）。",
                "游乐核销台账（amusement_verifications，实时查询）。",
                "amusement.verification.view",
                "amusement",
                true,
                false,
                "ADM-AMU-005",
                "ADM-AMU-005",
                "id",
                verificationColumns(),
                null));
        register(registry, new MetricDefinition(
                "amusement.exit_count",
                "游乐出场核销数",
                TYPE_ADDITIVE,
                "次",
                "出场方向核销逐条计数（direction='exit'，与游乐核销记录页「出场」卡一致）。"
                        + "可加口径=区间求和：按核销时间落游乐经营日窗计数；不传日期=全部历史。",
                "游乐核销台账（amusement_verifications，实时查询）。",
                "amusement.verification.view",
                "amusement",
                true,
                false,
                "ADM-AMU-005",
                "ADM-AMU-005",
                "id",
                verificationColumns(),
                null));

        // 安全巡检域（ADM-AMU-003 安全巡检/事故）：数字来自 GET /amusement/safety-inspections 平台口径全量列表。
        register(registry, new MetricDefinition(
                "amusement.inspection_count",
                "安全巡检数",
                TYPE_ADDITIVE,
                "次",
                "安全巡检逐条计数（含全部状态，与安全巡检/事故页「巡检数」卡一致）。可加口径=区间求和："
                        + "按创建时间落游乐经营日窗计数；不传日期=全部历史。",
                "安全巡检台账（amusement_safety_inspections，实时查询）。",
                "amusement.safety_inspection.view",
                "amusement",
                true,
                false,
                "ADM-AMU-003",
                "ADM-AMU-003",
                "id",
                safetyInspectionColumns(),
                null));
        register(registry, new MetricDefinition(
                "amusement.inspection_pending_rectification",
                "待整改巡检数",
                TYPE_BALANCE,
                "次",
                "当前待整改巡检数=整改状态（缺省回落总体结果）∈{pending,rectification_pending,pending_rectification} 的巡检计数"
                        + "（与安全巡检/事故页「待整改」卡一致）。余额口径=区间末值：取结束日期（缺省=当前游乐经营日）为截至日，"
                        + "起始日期不参与；仅限制创建时间早于截至日窗末。注意：整改状态按台账当前值计。",
                "安全巡检台账（amusement_safety_inspections，实时查询）。",
                "amusement.safety_inspection.view",
                "amusement",
                false,
                false,
                "ADM-AMU-003",
                "ADM-AMU-003",
                "id",
                safetyInspectionColumns(),
                null));

        // ==== 第四批（财务域，2026-07-19）：谓词逐字抄各页面现数字的既有取数（不自造口径）====
        // 应收应付台账（ADM-FIN-001）：应付欠款余额=应收欠费余额(finance.ar_balance)的应付镜像，as-of 口径同 ar_balance。
        register(registry, new MetricDefinition(
                "finance.ap_balance",
                "应付欠款余额",
                TYPE_BALANCE,
                "分",
                "逾期未清应付余额=Σ未付金额（分）（应付方向、未关闭、未付金额>0、到期日早于截至日）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前财务经营日）为截至日，起始日期不参与；"
                        + "与应收欠费余额(finance.ar_balance)同结构、方向取 payable。注意：明细按台账当前未付金额计，历史截至日仅影响到期日截止。",
                "应收应付台账（ar_ap_items，实时查询）。",
                "finance.ar_ap.view",
                "finance",
                false,
                false,
                "ADM-FIN-001",
                "ADM-FIN-001",
                "id",
                arApBalanceColumns("未付金额（分）"),
                null));
        // 欠费与回款（ADM-ANA-004）：坏账预警区=应收逾期90天以上余额，与看板 ETL ar.aging_90p 桶同谓词。
        register(registry, new MetricDefinition(
                "finance.ar_balance_aging90p",
                "坏账预警区余额",
                TYPE_BALANCE,
                "分",
                "应收逾期90天以上未清余额=Σ未收金额（分）（应收方向、未关闭、未收金额>0、逾期天数>90）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前财务经营日）为截至日，起始日期不参与；"
                        + "与欠费与回款页「坏账预警区」卡及看板 ETL ar.aging_90p（四桶最末桶）同一谓词同源。",
                "应收应付台账（ar_ap_items，实时查询）。",
                "finance.ar_ap.view",
                "finance",
                false,
                false,
                "ADM-ANA-004",
                "ADM-FIN-001",
                "id",
                arApBalanceColumns("未收金额（分）"),
                null));
        // 通票结算审阅（ADM-FIN-005）：数字来自 GET /settlement/transactions 平台口径列表（transaction_type='verification_settled'）。
        register(registry, new MetricDefinition(
                "settlement.transaction_count",
                "结算笔数",
                TYPE_ADDITIVE,
                "笔",
                "通票核销结算流水逐笔计数（transaction_type='verification_settled'，与通票结算审阅页「共 N 条」一致）。"
                        + "可加口径=区间求和：按发生时间落财务经营日窗计数；不传日期=全部历史（与页面默认无筛选全量列表一致）。",
                "结算流水台账（settlement_transactions，实时查询）。",
                "finance.settlement.view",
                "finance",
                true,
                false,
                "ADM-FIN-005",
                "ADM-FIN-005",
                "id",
                settlementColumns(),
                null));
        register(registry, new MetricDefinition(
                "settlement.verified_amount",
                "核销金额合计",
                TYPE_ADDITIVE,
                "分",
                "通票核销结算已核销金额合计（Σverified_amount_cent，同 transaction_type='verification_settled' 谓词，"
                        + "与结算审阅页各行「核销金额」列直加一致）。可加口径=区间求和：按发生时间落财务经营日窗；不传日期=全部历史。",
                "结算流水台账（settlement_transactions，实时查询）。",
                "finance.settlement.view",
                "finance",
                true,
                false,
                "ADM-FIN-005",
                "ADM-FIN-005",
                "id",
                settlementColumns(),
                null));
        register(registry, new MetricDefinition(
                "settlement.verified_count",
                "核销张数合计",
                TYPE_ADDITIVE,
                "张",
                "通票核销结算已核销张数合计（Σverified_count，同 transaction_type='verification_settled' 谓词，"
                        + "与结算审阅页各行「核销张数」列直加一致）。可加口径=区间求和：按发生时间落财务经营日窗；不传日期=全部历史。",
                "结算流水台账（settlement_transactions，实时查询）。",
                "finance.settlement.view",
                "finance",
                true,
                false,
                "ADM-FIN-005",
                "ADM-FIN-005",
                "id",
                settlementColumns(),
                null));

        // ==== 第五批（人事域，2026-07-19）：谓词逐字抄各页面既有取数（不自造口径）====
        // 说明：人事三页原为纯列表（无 KPI 卡）；本批为各页新增「平台口径」汇总卡（数字=其明细列表的 COUNT/SUM），
        // 谓词逐字同各列表端点（listShifts/listAttendanceOn/listPayrollStatements/listLeaves/listUsers 的平台口径=空 project_scope）。
        // 端点 requirePlatformScope 拦商户级/外包供应商（放行项目级）：本批指标均为平台口径聚合。
        // 项目级操作者（持 analytics.dashboard.view）取到的是平台口径数字（卡片已标注「平台口径」，不冒充其项目数），
        // 与其项目过滤后的页内列表口径不同——这是全站分析端点的框架级边界（与批4财务共用），非本批引入。
        // 排班与考勤（ADM-STF-001）：班次按 work_date 落 [起,止] 闭区间；考勤按 punched_at 纯自然日窗（同页 listAttendanceOn 的 00:00 UTC 日窗，不走经营日切）。
        register(registry, new MetricDefinition(
                "staff.shift_count",
                "排班班次数",
                TYPE_ADDITIVE,
                "班",
                "排班班次逐条计数（与排班与考勤页「排班」列表一致）。可加口径=区间求和：按排班日（work_date）落 [起,止] 闭区间计数；"
                        + "缺省=当前日（卡片传 from=to=页面所选日期，明细合计=该日列表条数）。平台口径=空项目范围（同页列表在平台管理员下的全量）。",
                "排班台账（staff_shift_schedules，实时查询）。",
                "staff.shift.manage",
                "staff",
                false,
                false,
                "ADM-STF-001",
                "ADM-STF-001",
                "id",
                staffShiftColumns(),
                null));
        register(registry, new MetricDefinition(
                "staff.attendance_count",
                "考勤打卡数",
                TYPE_ADDITIVE,
                "次",
                "考勤打卡逐条计数（含正常/迟到/早退/无班次全部判定，与排班与考勤页「当日考勤记录」列表一致）。"
                        + "可加口径=区间求和：按打卡时间（punched_at）落自然日窗 [起 00:00, 止次日 00:00) UTC 计数"
                        + "（与页面 listAttendanceOn 同一自然日窗，刻意不走经营日切）；缺省=当前日。平台口径=空项目范围。",
                "考勤台账（staff_attendance_records，实时查询）。",
                "staff.attendance.view",
                "staff",
                false,
                false,
                "ADM-STF-001",
                "ADM-STF-001",
                "id",
                staffAttendanceColumns(),
                null));
        register(registry, new MetricDefinition(
                "staff.attendance_late_count",
                "迟到打卡数",
                TYPE_ADDITIVE,
                "次",
                "判定为「迟到」（judgement='late'）的打卡逐条计数（与考勤列表「判定」列迟到行一致）。可加口径=区间求和："
                        + "按打卡时间落自然日窗（同考勤打卡数），加 judgement='late'；缺省=当前日。平台口径=空项目范围。",
                "考勤台账（staff_attendance_records，实时查询）。",
                "staff.attendance.view",
                "staff",
                false,
                false,
                "ADM-STF-001",
                "ADM-STF-001",
                "id",
                staffAttendanceColumns(),
                null));
        // 薪酬与请假（ADM-STF-002）：薪资单按 month 字符串落区间（YYYY-MM 字典序即时序）；待审请假=当前 pending 计数（余额）。
        register(registry, new MetricDefinition(
                "staff.payroll_net_total",
                "实发合计",
                TYPE_ADDITIVE,
                "分",
                "已月结薪资单实发金额合计（Σtotal_cent，与薪酬与请假页「当月薪资单」各行「实发合计」列直加一致）。"
                        + "可加口径=区间求和：按薪资单所属月份（month，YYYY-MM 字典序即时序）落 [起月,止月] 闭区间；"
                        + "缺省=当前月（卡片传 from=to=所选月首日 → 单月合计）。平台口径=空项目范围（同页列表在平台管理员下的全量，不加状态过滤）。",
                "薪资单台账（staff_payroll_statements，月结生成，实时查询）。",
                "staff.payroll.manage",
                "staff",
                false,
                false,
                "ADM-STF-002",
                "ADM-STF-002",
                "id",
                staffPayrollColumns(),
                null));
        register(registry, new MetricDefinition(
                "staff.payroll_statement_count",
                "薪资单数",
                TYPE_ADDITIVE,
                "份",
                "已月结薪资单逐份计数（与薪酬与请假页「当月薪资单」列表一致）。可加口径=区间求和：按所属月份落 [起月,止月] 闭区间；"
                        + "缺省=当前月。平台口径=空项目范围。",
                "薪资单台账（staff_payroll_statements，实时查询）。",
                "staff.payroll.manage",
                "staff",
                false,
                false,
                "ADM-STF-002",
                "ADM-STF-002",
                "id",
                staffPayrollColumns(),
                null));
        register(registry, new MetricDefinition(
                "staff.pending_leave_count",
                "待审请假数",
                TYPE_BALANCE,
                "笔",
                "当前待审请假数=状态为「待审」（status='pending'）的请假申请计数（与薪酬与请假页「待审请假」列表一致）。"
                        + "余额口径=区间末值：取结束日期（缺省=当前经营日）为截至日，仅限制申请提交时间早于截至日自然日窗末；起始日期不参与。"
                        + "注意：状态按台账当前值计。待审请假由排班主管审批，权限=staff.shift.manage（同 listLeaves 端点）。平台口径=空项目范围。",
                "请假台账（staff_leave_requests，实时查询）。",
                "staff.shift.manage",
                "staff",
                false,
                false,
                "ADM-STF-002",
                "ADM-STF-002",
                "id",
                staffLeaveColumns(),
                null));
        // 员工账号（ADM-ACC-001）：在职员工数=员工类账号且启用（余额）。
        register(registry, new MetricDefinition(
                "staff.active_headcount",
                "在职员工数",
                TYPE_BALANCE,
                "人",
                "在职员工数=账号类型为「普通员工」（account_type='staff'）且状态为「启用」（status='active'）的账号计数"
                        + "（与员工账号页员工类启用账号一致）。余额口径=区间末值：取结束日期（缺省=当前经营日）为截至日，"
                        + "仅计入建号时间早于截至日自然日窗末的账号；状态按台账当前值计。仅统计员工类账号，不含外包/商户/平台管理账号。平台口径=空项目范围。",
                "账号台账（access_users，实时查询）。",
                "access.user.view",
                "staff",
                false,
                false,
                "ADM-ACC-001",
                "ADM-ACC-001",
                "id",
                staffHeadcountColumns(),
                null));
        return registry;
    }

    private static List<MetricColumn> ticketOrderColumns() {
        return List.of(
                new MetricColumn("orderNo", "订单号"),
                new MetricColumn("ticketTypeId", "票种"),
                new MetricColumn("quantity", "数量"),
                new MetricColumn("salesChannel", "销售渠道"),
                new MetricColumn("amountCent", "金额（分）"),
                new MetricColumn("payStatus", "支付状态"),
                new MetricColumn("issueStatus", "出票状态"),
                new MetricColumn("createdAt", "下单时间"));
    }

    private static List<MetricColumn> sessionColumns() {
        return List.of(
                new MetricColumn("projectId", "项目"),
                new MetricColumn("startTime", "开始时间"),
                new MetricColumn("endTime", "结束时间"),
                new MetricColumn("capacity", "容量"),
                new MetricColumn("usedCapacity", "已用"),
                new MetricColumn("status", "状态"),
                new MetricColumn("createdAt", "创建时间"));
    }

    private static List<MetricColumn> groupBookingColumns() {
        return List.of(
                new MetricColumn("bookingType", "类型"),
                new MetricColumn("customerName", "客户"),
                new MetricColumn("partySize", "人数"),
                new MetricColumn("reservedCapacity", "占用容量"),
                new MetricColumn("bookingStatus", "状态"),
                new MetricColumn("createdAt", "下单时间"),
                new MetricColumn("paidAt", "确认时间"));
    }

    private static List<MetricColumn> verificationColumns() {
        return List.of(
                new MetricColumn("projectId", "项目"),
                new MetricColumn("sessionId", "场次"),
                new MetricColumn("direction", "方向"),
                new MetricColumn("verificationResult", "核销结果"),
                new MetricColumn("verifiedPeople", "核销人数"),
                new MetricColumn("createdAt", "核销时间"));
    }

    private static List<MetricColumn> safetyInspectionColumns() {
        return List.of(
                new MetricColumn("projectId", "项目"),
                new MetricColumn("deviceId", "设备"),
                new MetricColumn("overallResult", "总体结果"),
                new MetricColumn("inspectionStatus", "巡检状态"),
                new MetricColumn("rectificationStatus", "整改状态"),
                new MetricColumn("reopenStatus", "复检/重开状态"),
                new MetricColumn("createdAt", "创建时间"));
    }

    private static List<MetricColumn> barOrderColumns() {
        return List.of(
                new MetricColumn("id", "订单号"),
                new MetricColumn("tableId", "卡座"),
                new MetricColumn("merchantId", "商户"),
                new MetricColumn("status", "订单状态"),
                new MetricColumn("totalAmountCent", "订单金额（分）"),
                new MetricColumn("createdAt", "下单时间"),
                new MetricColumn("paidAt", "支付时间"));
    }

    private static List<MetricColumn> savedBottleColumns() {
        return List.of(
                new MetricColumn("id", "存酒号"),
                new MetricColumn("bottleName", "酒品"),
                new MetricColumn("initialQuantity", "存入数量"),
                new MetricColumn("remainingQuantity", "剩余数量"),
                new MetricColumn("unit", "单位"),
                new MetricColumn("status", "状态"),
                new MetricColumn("createdAt", "存入时间"),
                new MetricColumn("expireAt", "到期时间"));
    }

    private static List<MetricColumn> electricityColumns() {
        return List.of(
                new MetricColumn("meterName", "表计"),
                new MetricColumn("period", "抄表期"),
                new MetricColumn("consumption", "用量（kWh）"),
                new MetricColumn("abnormalReason", "异常原因"),
                new MetricColumn("readAt", "抄表时间"));
    }

    private static List<MetricColumn> arApBalanceColumns(String unpaidLabel) {
        return List.of(
                new MetricColumn("id", "单据号"),
                new MetricColumn("merchantId", "往来单位"),
                new MetricColumn("sourceType", "费用类型"),
                new MetricColumn("dueDate", "到期日"),
                new MetricColumn("unpaidAmountCent", unpaidLabel),
                new MetricColumn("status", "状态"));
    }

    private static List<MetricColumn> settlementColumns() {
        return List.of(
                new MetricColumn("transactionNo", "流水号"),
                new MetricColumn("projectId", "项目"),
                new MetricColumn("merchantId", "商户"),
                new MetricColumn("entitlementType", "权益类型"),
                new MetricColumn("verifiedCount", "核销张数"),
                new MetricColumn("numPeople", "人数"),
                new MetricColumn("verifiedAmountCent", "核销金额（分）"),
                new MetricColumn("settlementStatus", "结算状态"),
                new MetricColumn("createdAt", "发生时间"));
    }

    private static List<MetricColumn> staffShiftColumns() {
        return List.of(
                new MetricColumn("userId", "员工"),
                new MetricColumn("workDate", "排班日"),
                new MetricColumn("startAt", "开始"),
                new MetricColumn("endAt", "结束"),
                new MetricColumn("note", "备注"),
                new MetricColumn("id", "排班编号"));
    }

    private static List<MetricColumn> staffAttendanceColumns() {
        return List.of(
                new MetricColumn("userId", "员工"),
                new MetricColumn("punchType", "类型"),
                new MetricColumn("punchedAt", "打卡时间"),
                new MetricColumn("method", "方式"),
                new MetricColumn("judgement", "判定"),
                new MetricColumn("spaceId", "点位"),
                new MetricColumn("id", "考勤编号"));
    }

    private static List<MetricColumn> staffPayrollColumns() {
        return List.of(
                new MetricColumn("userId", "员工"),
                new MetricColumn("month", "月份"),
                new MetricColumn("baseCent", "固定工资（分）"),
                new MetricColumn("commissionCent", "提成（分）"),
                new MetricColumn("bonusCent", "奖金（分）"),
                new MetricColumn("penaltyCent", "处罚（分）"),
                new MetricColumn("totalCent", "实发合计（分）"),
                new MetricColumn("id", "薪资单号"));
    }

    private static List<MetricColumn> staffLeaveColumns() {
        return List.of(
                new MetricColumn("userId", "员工"),
                new MetricColumn("leaveType", "请假类型"),
                new MetricColumn("startDate", "开始"),
                new MetricColumn("endDate", "结束"),
                new MetricColumn("reason", "事由"),
                new MetricColumn("status", "状态"),
                new MetricColumn("id", "申请单号"));
    }

    private static List<MetricColumn> staffHeadcountColumns() {
        return List.of(
                new MetricColumn("username", "账号"),
                new MetricColumn("accountType", "账号类型"),
                new MetricColumn("status", "状态"),
                new MetricColumn("createdAt", "创建时间"),
                new MetricColumn("id", "账号编号"));
    }

    private static void register(Map<String, MetricDefinition> registry, MetricDefinition definition) {
        registry.put(definition.metricKey(), definition);
    }

    /** 按指标编码取登记项；未登记 → 校验失败（fail-closed，不猜口径）。 */
    public static MetricDefinition require(String metricKey) {
        MetricDefinition definition = metricKey == null ? null : REGISTRY.get(metricKey.trim());
        if (definition == null) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "未登记的指标编码：" + metricKey + "（已登记：" + String.join("、", REGISTRY.keySet()) + "）");
        }
        return definition;
    }

    public static Collection<MetricDefinition> all() {
        return REGISTRY.values();
    }

    private MetricRegistry() {
    }
}
