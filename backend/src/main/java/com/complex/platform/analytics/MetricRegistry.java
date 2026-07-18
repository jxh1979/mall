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
 * 首批登记（覆盖三类口径）均为平台口径，暂不支持商户/项目维度过滤（登记 supportsMerchantDim=false）。
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
        return registry;
    }

    private static List<MetricColumn> electricityColumns() {
        return List.of(
                new MetricColumn("meterName", "表计"),
                new MetricColumn("period", "抄表期"),
                new MetricColumn("consumption", "用量（kWh）"),
                new MetricColumn("abnormalReason", "异常原因"),
                new MetricColumn("readAt", "抄表时间"));
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
