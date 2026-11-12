package com.complex.platform.settlement;

// 文件版本：V0.06

import com.complex.platform.common.api.ApiException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 结算重构批2-3:月度通票分账 读配置 + 读当月核销聚合 + upsert 结算单/明细。
 *
 * <p>配置读取范式照 payroll_tax_brackets / merchant_revenue_attribution_rules：
 * {@code COALESCE(project_id,'__DEFAULT__')} 查「该项目特例 or 全局默认」，
 * 无任何配置行则回退 SQL 内的建表默认值（DEFAULT ...），保证零配置也能跑出默认口径。
 *
 * <p>批2-3 只写 draft 结算单 + 明细，<b>不过账 ar_ap_items</b>（批2-4）。
 */
// ponytail: 单聚合服务的读写仓储，无接口（one impl）；过账能力出现在批2-4 再拆。
@Repository
public class JdbcSettlementBillRepository implements com.complex.platform.ticketing.ComboPriceToleranceConfigPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /** 池分账的三种方式（不含专属直付与 FS-021-R2 追收明细）：越池判定与超额两段拆只认这三种。 */
    static final String POOL_METHODS_SQL = "'by_rank_tier','by_venue_per_guest','by_fixed_amount'";

    /** 冲正行所冲原核销（ST-VS 行）的业务时刻。 */
    private static final String ORIGINAL_AT = "(SELECT COALESCE(o.business_at, o.created_at) FROM settlement_transactions o"
            + " WHERE o.transaction_no = 'ST-VS-' || st.verification_id)";

    /**
     * 月窗（FS-021-R2，操作员裁决 2026-10-19 第 4 条「单独列追收、按原月重算」）：按 business_at 落窗，但原月已结付后才撤销的
     * 核销——冲正行落在当月、其原核销早于本窗——不计入当月（当月应付照常全额），改由原月口径另列追收。
     * :recovery_cutoff 非空＝原月重算：本窗即原月，另并入原核销在本窗、冲正落在本窗之后且早于 cutoff 的冲正行。
     */
    private static final String WINDOW = """
               AND ((COALESCE(st.business_at, st.created_at) >= :start AND COALESCE(st.business_at, st.created_at) < :end
                     AND NOT (st.transaction_type = 'verification_reversed' AND COALESCE(%1$s < :start, FALSE)))
                 OR (st.transaction_type = 'verification_reversed'
                     AND COALESCE(st.business_at, st.created_at) >= :end
                     AND COALESCE(st.business_at, st.created_at) < CAST(:recovery_cutoff AS timestamptz)
                     AND %1$s >= :start AND %1$s < :end))
            """.formatted(ORIGINAL_AT);

    /**
     * 第十八批第 2 条（ENUM L52「池金额在售票时随订单固化」）：父票有池快照、提供方为酒吧业态的核销——其金额与扣项由引擎从池快照逐份取，
     * 不再进按核销聚合的收入、综合服务费、赠送权益预估成本、通路费与销售渠道费口径（否则同一张票的酒吧份额算两遍）。
     * %1$s＝核销行别名，%2$s＝商户别名（同 {@link #aggregateFormatRevenue} 的业态兜底）。
     */
    private static final String POOLED_AT_SALE = """
            (COALESCE(%2$s.merchant_type, 'bar') = 'bar'
             AND EXISTS (SELECT 1 FROM pass_pool_snapshots pps WHERE pps.ticket_instance_id = %1$s.parent_ticket_id))""";

    /**
     * 单一酒吧门票行（ENUM:32「单一酒吧门票由对应酒吧商户定价、销售并确认已核销部分的收入」）：订单挂酒吧，其核销按专属直付记到订单酒吧
     * （见 SettlementSettledConsumer 专属商户取订单酒吧）。%1$s＝结算事实行别名。第十八批 AI 段 216-R1、212-R1 共用。
     */
    static final String SINGLE_BAR_ROW =
            "EXISTS (SELECT 1 FROM ticket_orders sbo WHERE sbo.id = %1$s.order_id AND sbo.bar_id = %1$s.exclusive_merchant_id)";

    /**
     * 归属主体自收的票（第十八批 AI 段 237-R1、214-R2；ENUM:32「纯游乐票和纯游乐通票由游乐公司负责」「演出票由传媒公司负责」；BSM:1720「与娱乐平台公司无关」；
     * BSM:1144「由传媒公司自行定价、售票、收款」）：本行订单里有价的权益全在本行业态——与「该行业态是游乐或传媒」合用即纯游乐票、纯游乐通票与传媒自办演出票。
     * %1$s＝结算事实行别名，%2$s＝该行业态表达式。
     */
    static final String OWN_COLLECTED = """
            NOT EXISTS (SELECT 1 FROM issued_entitlements oe LEFT JOIN merchants om ON om.id = oe.merchant_id
                         WHERE oe.order_id = %1$s.order_id AND oe.allocated_revenue_cent > 0
                           AND COALESCE(om.merchant_type, 'bar') <> %2$s)""";

    public JdbcSettlementBillRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---- 读配置（COALESCE 默认+项目特例；无配置回退建表默认值）----
    // S4:原「留存万分比」finder 已删——池内留存与门票综合服务费为同一项、只扣一次(业务 HTML),
    //   费率统一走 findComprehensiveServiceFee(platform_retention_rules 已随 V370 停用)。

    /** 第三方渠道抽成配置（通用配置，channel 未知/无订单时用）。 */
    public ChannelFeeConfig findChannelFee(String projectId) {
        return findChannelFee(projectId, null);
    }

    /**
     * S11②/V381：某<b>第三方销售渠道</b>的销售渠道费配置——比例(万分比)或<b>每张票固定金额</b>(分)二选一
     * (chk_cfc_fee_mode 保证不双配)。优先级：项目特例 &gt; 全局默认；同级下 该渠道专属配置 &gt; 通用配置。
     * 渠道无专属配置 → 落回通用配置。第十八批第 3 条（V555）：只读销售渠道费层（fee_layer='sales'）；
     * 官方渠道售出的票不扣销售渠道费（BSM L749），由调用方按 {@code TicketOrderService.OFFICIAL_SALES_CHANNELS} 跳过。
     */
    public ChannelFeeConfig findChannelFee(String projectId, String channel) {
        ChannelFeeConfig cfg = jdbcTemplate.query("""
                SELECT id, fee_bps, fixed_amount_cent FROM channel_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE AND fee_layer = 'sales'
                   AND (channel IS NULL OR channel = :channel)
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC, (channel IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                        .addValue("project_id", projectId)
                        .addValue("channel", channel),
                rs -> rs.next()
                        ? new ChannelFeeConfig(rs.getString("id"), rs.getInt("fee_bps"),
                                rs.getObject("fixed_amount_cent") == null ? null : rs.getLong("fixed_amount_cent"))
                        : null);
        return requireConfigured(cfg, "channel_fee_configs", projectId);
    }

    /**
     * 第十八批第 3 条（V555）：某<b>支付通路</b>的通路费率（fee_layer='route'，只按比例）。只认该通路的专属行，
     * 项目特例 &gt; 全局默认；未配置 = 费率 0、无配置行（与储值充值批次固化费率同口径）。
     */
    public RouteFee findRouteFee(String projectId, String paymentChannel) {
        RouteFee fee = jdbcTemplate.query("""
                SELECT id, fee_bps FROM channel_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE AND fee_layer = 'route' AND channel = :channel
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId).addValue("channel", paymentChannel),
                rs -> rs.next() ? new RouteFee(rs.getString("id"), rs.getInt("fee_bps")) : null);
        return fee == null ? new RouteFee(null, 0) : fee;
    }

    /**
     * 第 75 条（ENUM L52「按费率最高的支付通路计取通路费」）：各支付通路在该项目下生效的通路费率（每个通路项目特例＞全局默认，同
     * {@link #findRouteFee}）中的最高值；未配置＝0。储值付款的通路费来自充值时的支付通路，同属这些行。
     */
    public int findMaxRouteFeeBps(String projectId) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(fee_bps), 0) FROM (
                    SELECT DISTINCT ON (channel) fee_bps FROM channel_fee_configs
                     WHERE status = 'active' AND is_draft = FALSE AND fee_layer = 'route'
                       AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                     ORDER BY channel, (project_id IS NOT NULL) DESC) r
                """, new MapSqlParameterSource("project_id", projectId), Integer.class);
    }

    /** 分配方式：项目特例优先，否则全局默认，否则建表默认 by_rank_tier。 */
    public String findDistributionMethod(String projectId) {
        String method = jdbcTemplate.query("""
                SELECT method_type FROM ticket_distribution_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                rs -> rs.next() ? rs.getString("method_type") : null);
        return requireConfigured(method, "ticket_distribution_configs.method_type", projectId);
    }

    /**
     * 第 75 条（ENUM L52「初始有效期不超过后台配置的上限（业务参数，按项目可配，出厂默认三个月，须同时配置上下限）」；
     * BSM L752「单次激励期限不得超过后台配置的上限」）：短期经营激励单次有效期上限——项目特例优先，否则全局默认（出厂 3 个月）。
     * 读回「数值 + 单位」，单位 day/month（V368）；该参数自身的上下限只在写入时校验。
     */
    public SpecialApprovalTerm findSpecialApprovalTerm(String projectId) {
        SpecialApprovalTerm term = jdbcTemplate.query("""
                SELECT special_approval_term_value, special_approval_term_unit FROM ticket_distribution_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                rs -> rs.next()
                        ? new SpecialApprovalTerm(rs.getInt("special_approval_term_value"),
                                rs.getString("special_approval_term_unit"))
                        : null);
        return requireConfigured(term, "ticket_distribution_configs.special_approval_term_value", projectId);
    }

    /**
     * 第一段规则二选一取值（与 V556 chk_tdfs_rule 一致）。第十八批第 2 条起第一段名单与金额随分账方案版本在售票时固化进池快照，
     * 旧单梯子 ticket_distribution_rank_tiers 与月度口径 ticket_distribution_merchant_rates 停读（铁律 8 不换算）。
     */
    public static final String RULE_PER_GUEST = "per_guest";
    public static final String RULE_FIXED = "fixed";

    // ---- 读当月核销聚合（批1 settlement_transactions，verification_settled，按 created_at 落窗）----

    /**
     * 当月<b>进池</b>核销合计:收入 = Σ verified_amount_cent,综合服务费(池链支)= Σ comp_service_fee_cent,
     * <b>仅非专属</b>（exclusive_merchant_id IS NULL）。专属某商户核销全额直付该商户，不进池（批3-4 §6.4）。
     * 月窗 [monthStart, nextMonthStart) 按 business_at(核销/冲正业务时刻)落窗,冲正对配天然净掉。
     *
     * <p><b>S4 两个 SUM 必须同一条查询</b>:池链综合服务费(F_pool)与池基收入必须复用完全相同的过滤条件
     * (严格二分,不另写查询)——两处各写各的迟早口径漂移,费被双记或漏记时对账查不出来。
     */
    public PooledTotals sumPooledTotals(String projectId, Instant monthStart, Instant nextMonthStart) {
        return sumPooledTotals(projectId, monthStart, nextMonthStart, null);
    }

    public PooledTotals sumPooledTotals(String projectId, Instant monthStart, Instant nextMonthStart, Instant recoveryCutoff) {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(st.verified_amount_cent), 0)  AS revenue_cent,
                       COALESCE(SUM(st.comp_service_fee_cent), 0) AS comp_service_fee_cent,
                       COALESCE(SUM(st.gift_cost_cent), 0)        AS gift_cost_cent,
                       COALESCE(SUM(st.route_fee_cent), 0)        AS route_fee_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NULL
                   AND NOT """ + POOLED_AT_SALE.formatted("st", "m") + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff),
                (rs, i) -> new PooledTotals(rs.getLong("revenue_cent"), rs.getLong("comp_service_fee_cent"),
                        rs.getLong("gift_cost_cent"), rs.getLong("route_fee_cent")));
    }

    /**
     * S11①：当月<b>进池</b>核销按【业态】汇总（毛额侧 Σverified、折扣侧 Σdiscount）。
     *
     * <p>业态取自<b>提供该权益的商户</b>的业态（{@code merchants.merchant_type}，V348 五值封口）——
     * 谁把这份东西给了客人，这份钱就算谁那个业态的。与 {@link #aggregateMerchantStats} 的业态条件同一套，
     * 保证「排名/客数只算酒吧」和「份额只把酒吧那份进池」说的是同一批核销。
     *
     * <p>业态是招商录入商户时的必填主数据、且商户只软删不硬删 → 线上恒能解析到。
     * ponytail: 解析不到（测试夹具直插流水、未建商户行）按酒吧计＝改造前口径，不静默丢钱；
     * 真要 fail-loud 需先把各处测试夹具补齐商户行，届时把 COALESCE 去掉即可。
     */
    public List<FormatRevenue> aggregateFormatRevenue(String projectId, Instant monthStart, Instant nextMonthStart,
                                                      Instant recoveryCutoff) {
        return formatRevenue(projectId, monthStart, nextMonthStart, recoveryCutoff, "");
    }

    /**
     * 第十八批 AI 段 237-R1（ENUM:32「收款主体不等于收入归属主体」「纯游乐票和纯游乐通票由游乐公司负责」；BSM:1720「与娱乐平台公司无关」；
     * BSM:1144「由传媒公司自行定价、售票、收款」）：{@link #aggregateFormatRevenue} 同一批进池核销中，归属主体自收的游乐、传媒行（{@link #OWN_COLLECTED}）
     * 按业态汇总——这部分不是娱乐平台公司代收，不进域间份额。
     */
    public List<FormatRevenue> aggregateOwnCollectedFormatRevenue(String projectId, Instant monthStart, Instant nextMonthStart,
                                                                  Instant recoveryCutoff) {
        return formatRevenue(projectId, monthStart, nextMonthStart, recoveryCutoff,
                "   AND COALESCE(m.merchant_type, 'bar') IN ('amusement','media')\n   AND "
                        + OWN_COLLECTED.formatted("st", "COALESCE(m.merchant_type, 'bar')") + "\n");
    }

    private List<FormatRevenue> formatRevenue(String projectId, Instant monthStart, Instant nextMonthStart,
                                              Instant recoveryCutoff, String extraCondition) {
        // S4:各业态行带上本业态计提的综合服务费——费跟着核销走,游乐/餐饮份额的费从各自净份额扣,
        //   不错扣进酒吧池;Σ各业态费 与 sumPooledTotals 的费合计由引擎 K20 运行期互核。
        // S6:赠送权益预估成本(G)同一范式跟核销走——计提落在哪行核销上就扣在那行所属业态的份额里,
        //   别的业态不替它埋单;常态首笔核销=入场(酒吧业态)→G 落酒吧行,与 HTML:742 瀑布顺序一致。
        return jdbcTemplate.query("""
                SELECT COALESCE(m.merchant_type, 'bar')            AS business_format,
                       COALESCE(SUM(st.verified_amount_cent), 0)   AS revenue_cent,
                       COALESCE(SUM(st.discount_amount_cent), 0)   AS discount_cent,
                       COALESCE(SUM(st.comp_service_fee_cent), 0)  AS comp_service_fee_cent,
                       COALESCE(SUM(st.gift_cost_cent), 0)         AS gift_cost_cent,
                       COALESCE(SUM(st.route_fee_cent), 0)         AS route_fee_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NULL
                   AND NOT """ + POOLED_AT_SALE.formatted("st", "m") + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                """ + extraCondition + """
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new FormatRevenue(rs.getString("business_format"),
                        rs.getLong("revenue_cent"), rs.getLong("discount_cent"),
                        rs.getLong("comp_service_fee_cent"), rs.getLong("gift_cost_cent"),
                        rs.getLong("route_fee_cent")));
    }

    /**
     * 计提串行门(2026-08-03):对某个「只能收一道」的口子取<b>事务级</b>咨询锁。
     *
     * <p>业务上解决的是:同一张票的两次核销几乎同时提交时,两边都读到「这张票还没收过」,
     * 于是一张票收了两道综合服务费/预扣了两道赠送成本 —— 对账时一张票冒出两条同名费,
     * 业务人员解释不了,而且多收的那一道会真的进分账。
     *
     * <p><b>为什么是咨询锁而不是唯一索引</b>:业务口径明写「冲正后该票可重新收一道」
     * (见 {@link #sumTicketServiceFeeCent}),唯一索引会把这条合法重收永久堵死;且月度封顶桶是跨票的,
     * 唯一索引对它无效。消费者本就是 REQUIRES_NEW 独立事务 → 事务级锁随提交/回滚自动释放,
     * 零 schema 变更、无需显式解锁、进程崩也不会留死锁。
     *
     * <p><b>取锁顺序固定</b>「父票 → 费率规则 → G 规则」(调用方保证),不交叉取锁 → 不会死锁。
     */
    public void lockAccrualKey(String key) {
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(hashtext(:key))",
                new MapSqlParameterSource().addValue("key", key), rs -> null);
    }

    /**
     * 提供方商户的<b>经营业态</b>(HTML「门票综合服务费只适用于酒吧相关票务」的判定源)。
     *
     * <p><b>与 {@link #aggregateFormatRevenue} 同源</b>:同一张表同一列(merchants.merchant_type),
     * 商户行缺失(无提供方/未建档)时同样兜底 {@code 'bar'} —— 两处判据若不同源,同一笔核销会出现
     * 「计提时按酒吧收了费、月结时归到游乐份额」,K20 业态守恒当场炸,而账面数字看着还「差不多对」。
     */
    public String merchantBusinessFormat(String merchantId) {
        if (merchantId == null) {
            return TicketDistributionSettlementService.FORMAT_BAR;
        }
        String format = jdbcTemplate.query(
                "SELECT merchant_type FROM merchants WHERE id = :id",
                new MapSqlParameterSource().addValue("id", merchantId),
                rs -> rs.next() ? rs.getString(1) : null);
        return format == null ? TicketDistributionSettlementService.FORMAT_BAR : format;
    }

    /**
     * S11②：当月<b>进池</b>核销按【销售渠道】汇总实付额（渠道抽成整单前移的计费基数）。
     * 渠道取自出票订单 {@code ticket_orders.sales_channel}；direct 出票无订单 → 空串（落通用率）。
     *
     * <p><b>V381 当期票数(固定金额模式计费基数)</b>：每张票只在其「首笔进池核销」(锚点=该父票最早的
     * verification_settled 行)所在月计 +1——一张票多次核销不重复计(与 S3 综合服务费固定额「一张票只收一道」
     * 同语义)；冲正行沿用原核销的 verification_id，冲的恰是锚点行时计 −1(冲正取反)，冲非锚点行不动票数。
     * 无 parent_ticket_id 的行(V285 前不合规发放数据)不计票数——与 S3 同一态度，绝不退化成按核销笔数各收一道。
     * 第十八批第 2 条：有池快照的票，其酒吧那部分的销售渠道费已按酒吧份额在售票时固化进池快照——比例模式的计费基数不再含这些酒吧核销额；
     * 固定金额模式票数锚点照旧，另给出锚点票快照里已扣的酒吧那部分（pooledAtSaleFeeCent），由引擎扣回，整票仍只扣一次。
     * 第十八批第 3 条（BSM L749「每笔分摊须记录所用合同版本」）：费率或固定额按订单付款时固化的配置（ticket_order_channel_fees）分组，
     * 不按结算当月生效的配置；官方渠道与没有固化行的订单（存量）不扣。
     * 第十八批 X 段 176-R2（BSM:749「两者一律在拆分给各域之前从整单扣除且只扣一次，由各域按分配份额自然共担」）：有价专属直付行（记了域份额）
     * 一并计入，比例基数取其域份额，锚点同样认它。
     */
    // ponytail: 锚点=全期最早核销、不随冲正复位——锚点被冲正后同一张票再核销(新 verificationId)不再计费,
    //   平台少收该票一道(客人有利方向,入场冲正后重入场属罕见);真出现再照 S3 净额复位语义升级。
    public List<ChannelRevenue> aggregateChannelRevenue(String projectId, Instant monthStart, Instant nextMonthStart) {
        return aggregateChannelRevenue(projectId, monthStart, nextMonthStart, null);
    }

    public List<ChannelRevenue> aggregateChannelRevenue(String projectId, Instant monthStart, Instant nextMonthStart,
                                                        Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT COALESCE(o.sales_channel, '')              AS sales_channel,
                       f.sales_fee_config_id, COALESCE(f.sales_fee_bps, 0) AS sales_fee_bps, f.sales_fixed_amount_cent,
                       COALESCE(SUM(CASE WHEN """ + POOLED_AT_SALE.formatted("st", "m") + """
                                         THEN 0 ELSE COALESCE(st.domain_share_cent, st.verified_amount_cent) END), 0) AS revenue_cent,
                       COALESCE(SUM(CASE WHEN a.verification_id IS NOT NULL
                                         THEN CASE st.transaction_type WHEN 'verification_settled' THEN 1 ELSE -1 END
                                         ELSE 0 END), 0)          AS ticket_count,
                       COALESCE(SUM(CASE WHEN a.verification_id IS NOT NULL
                                         THEN CASE st.transaction_type WHEN 'verification_settled' THEN 1 ELSE -1 END
                                              * COALESCE(ps.sales_channel_fee_cent, 0)
                                         ELSE 0 END), 0)          AS pooled_at_sale_fee_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                  LEFT JOIN pass_pool_snapshots ps ON ps.ticket_instance_id = st.parent_ticket_id
                  LEFT JOIN ticket_orders o ON o.id = st.order_id
                  LEFT JOIN ticket_order_channel_fees f ON f.order_id = st.order_id
                  LEFT JOIN (SELECT DISTINCT ON (parent_ticket_id) parent_ticket_id, verification_id
                               FROM settlement_transactions
                              WHERE transaction_type = 'verification_settled'
                                AND verification_id IS NOT NULL
                                AND (exclusive_merchant_id IS NULL OR domain_share_cent IS NOT NULL)
                                AND parent_ticket_id IS NOT NULL
                                AND (:project_id::text IS NULL OR project_id = :project_id)
                              ORDER BY parent_ticket_id, COALESCE(business_at, created_at), id) a
                         ON a.parent_ticket_id = st.parent_ticket_id AND a.verification_id = st.verification_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND (st.exclusive_merchant_id IS NULL OR st.domain_share_cent IS NOT NULL)
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1, 2, 3, 4
                 ORDER BY 1, 2
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new ChannelRevenue(rs.getString("sales_channel"),
                        new ChannelFeeConfig(rs.getString("sales_fee_config_id"), rs.getInt("sales_fee_bps"),
                                (Long) rs.getObject("sales_fixed_amount_cent")),
                        rs.getLong("revenue_cent"), rs.getLong("ticket_count"), rs.getLong("pooled_at_sale_fee_cent")));
    }

    /** 第十八批第 3 条：订单付款时固化的销售渠道费配置；官方渠道或没有固化行（存量）＝不扣（费率 0、无配置行）。 */
    public ChannelFeeConfig findOrderSalesChannelFee(String orderId) {
        return jdbcTemplate.query("""
                SELECT sales_fee_config_id, sales_fee_bps, sales_fixed_amount_cent FROM ticket_order_channel_fees
                 WHERE order_id = :order_id AND sales_fee_config_id IS NOT NULL
                """, new MapSqlParameterSource("order_id", orderId),
                rs -> rs.next() ? new ChannelFeeConfig(rs.getString(1), rs.getInt(2), (Long) rs.getObject(3)) : NO_SALES_FEE);
    }

    /** 不扣销售渠道费（官方渠道、存量订单）。 */
    static final ChannelFeeConfig NO_SALES_FEE = new ChannelFeeConfig(null, 0, null);

    /**
     * 第十八批 AI 段 212-R1（ENUM:51「所有支付都会产生通路费、官方渠道同样产生……不由该笔业务的收款主体单独承担」「任何票种经第三方渠道售出时，
     * 销售渠道费一律先从整单扣除且只扣一次」「每笔分摊的结果与所用合同版本必须留痕」；BSM:749「官方渠道售出的票不扣销售渠道费，但仍扣通路费」）：
     * 当月单一酒吧门票核销与冲正行按酒吧 × 订单付款时固化的销售渠道费配置汇总核销额、已分摊的通路费与固定额锚点票数（锚点写法同
     * {@link #aggregateChannelRevenue}：每张票只在其首笔核销所在月计 +1，冲的恰是锚点行计 −1）。官方渠道没有固化配置＝不扣销售渠道费。
     */
    public List<SingleBarFee> aggregateSingleBarFees(String projectId, Instant monthStart, Instant nextMonthStart,
                                                     Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT st.exclusive_merchant_id                    AS merchant_id,
                       f.sales_fee_config_id, COALESCE(f.sales_fee_bps, 0) AS sales_fee_bps, f.sales_fixed_amount_cent,
                       COALESCE(SUM(st.verified_amount_cent), 0) AS revenue_cent,
                       COALESCE(SUM(st.route_fee_cent), 0)       AS route_fee_cent,
                       COALESCE(SUM(CASE WHEN a.verification_id IS NOT NULL
                                         THEN CASE st.transaction_type WHEN 'verification_settled' THEN 1 ELSE -1 END
                                         ELSE 0 END), 0)          AS ticket_count
                  FROM settlement_transactions st
                  LEFT JOIN ticket_order_channel_fees f ON f.order_id = st.order_id
                  LEFT JOIN (SELECT DISTINCT ON (x.parent_ticket_id) x.parent_ticket_id, x.verification_id
                               FROM settlement_transactions x
                              WHERE x.transaction_type = 'verification_settled'
                                AND x.verification_id IS NOT NULL
                                AND x.domain_share_cent IS NULL
                """ + "                AND " + SINGLE_BAR_ROW.formatted("x") + "\n" + """
                                AND x.parent_ticket_id IS NOT NULL
                                AND (:project_id::text IS NULL OR x.project_id = :project_id)
                              ORDER BY x.parent_ticket_id, COALESCE(x.business_at, x.created_at), x.id) a
                         ON a.parent_ticket_id = st.parent_ticket_id AND a.verification_id = st.verification_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.domain_share_cent IS NULL
                """ + "   AND " + SINGLE_BAR_ROW.formatted("st") + "\n" + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1, 2, 3, 4
                 ORDER BY 1, 2
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new SingleBarFee(rs.getString("merchant_id"),
                        new ChannelFeeConfig(rs.getString("sales_fee_config_id"), rs.getInt("sales_fee_bps"),
                                (Long) rs.getObject("sales_fixed_amount_cent")),
                        rs.getLong("revenue_cent"), rs.getLong("route_fee_cent"), rs.getLong("ticket_count")));
    }

    /**
     * 当月各酒吧统计：按 merchant_id 聚合 核销数(Σverified_count) + 客数(Σnum_people)。仅归属商户的核销。
     * S11①：<b>只算酒吧业态</b>——游乐/餐饮/零售的份额不进酒吧池，自然也不该参与酒吧的排名与客数聚合，
     * 否则一家餐厅会因为核销了几张餐券而占掉某个酒吧的分池名次。业态条件与 {@link #aggregateFormatRevenue} 同源。
     * 第十八批第 2 条（ENUM L52「按各酒吧当期核销入场人数由多到少排定」）：只算入场类权益（entry/admission）、按入场人数降序，
     * 赠酒等其他权益的核销不计名次；有池快照的票的入场核销照常计入。
     */
    public List<MerchantMonthlyStat> aggregateMerchantStats(String projectId, Instant monthStart, Instant nextMonthStart,
                                                            Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT st.merchant_id,
                       COALESCE(SUM(st.verified_count), 0) AS verified_count,
                       COALESCE(SUM(st.num_people), 0)     AS guest_count
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.beneficiary_type = 'merchant'
                   AND st.merchant_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NULL
                   AND COALESCE(m.merchant_type, 'bar') = 'bar'
                   AND st.entitlement_type IN ('entry', 'admission')
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY st.merchant_id
                 ORDER BY guest_count DESC, st.merchant_id
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new MerchantMonthlyStat(rs.getString("merchant_id"),
                        rs.getInt("verified_count"), rs.getInt("guest_count")));
    }

    /**
     * 结算重构批3-4(§6.4)：当月<b>专属</b>核销按 exclusive_merchant_id 分组 Σ verified_amount_cent。
     * 每个专属商户一条 → 全额直付明细（method=exclusive_full，不进可分配池、不稀释他家分池）。
     * 按落定在 settlement_transactions 的 exclusive_merchant_id 过滤（非 join 活模板，历史结算不受改模板影响）。
     */
    public List<ExclusiveMerchantRevenue> aggregateExclusiveRevenue(String projectId, Instant monthStart, Instant nextMonthStart,
                                                                    Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT exclusive_merchant_id AS merchant_id,
                       entitlement_type,
                       COALESCE(SUM(verified_amount_cent), 0) AS revenue_cent,
                       COALESCE(SUM(discount_amount_cent), 0) AS discount_cent,
                       COALESCE(SUM(verified_count), 0)       AS verified_count,
                       COALESCE(SUM(num_people), 0)           AS guest_count,
                       COALESCE(SUM(comp_service_fee_cent), 0) AS comp_service_fee_cent
                  FROM settlement_transactions st
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND verification_id IS NOT NULL
                   AND exclusive_merchant_id IS NOT NULL
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                 GROUP BY exclusive_merchant_id, entitlement_type
                 ORDER BY exclusive_merchant_id, entitlement_type
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new ExclusiveMerchantRevenue(rs.getString("merchant_id"), rs.getString("entitlement_type"),
                        rs.getLong("revenue_cent"), rs.getLong("discount_cent"),
                        rs.getInt("verified_count"), rs.getInt("guest_count"),
                        rs.getLong("comp_service_fee_cent")));
    }

    /**
     * 第十八批 X 段 176-R2（BSM:749「再按各业态价格占比分到各域」「由各域按分配份额自然共担」；ENUM:53「按内部结算价向实际提供方足额结算并自行
     * 承担差额」）：当月有价专属直付核销与冲正行（记了域份额的）按专属商户业态汇总域份额、分摊的通路费与按内部结算价应直付额。
     * 零元权益不记域份额、不进（BSM:749「零元免费权益不参与域间分配」）。
     */
    public List<ExclusiveDomainShare> aggregateExclusiveDomainShares(String projectId, Instant monthStart, Instant nextMonthStart,
                                                                     Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT COALESCE(m.merchant_type, 'bar')          AS business_format,
                       COALESCE(SUM(st.domain_share_cent), 0)    AS domain_share_cent,
                       COALESCE(SUM(st.route_fee_cent), 0)       AS route_fee_cent,
                       COALESCE(SUM(st.verified_amount_cent), 0) AS internal_settled_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.exclusive_merchant_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.domain_share_cent IS NOT NULL
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new ExclusiveDomainShare(rs.getString("business_format"), rs.getLong("domain_share_cent"),
                        rs.getLong("route_fee_cent"), rs.getLong("internal_settled_cent")));
    }

    /**
     * P6 增量封顶:某专属商户×费率规则 在 [monthStart,nextMonthStart) 内【已落】的实收综合服务费累计
     * (核销正 + 冲正负 → 冲正天然释放封顶额度)。核销时算本笔前先查此累计,remaining = max(0, cap - mtd)。
     * business_at 落月窗(与聚合口径一致);不含本笔(本笔尚未 insert)。
     *
     * <p><b>S2 覆盖面扩到「所有门票」后</b>:非专属(进池)核销也计提,这类行 exclusive_merchant_id 为 NULL,
     * 原 {@code = :merchant_id} 对 NULL 恒不成立 → 进池那部分的封顶会形同虚设(月内可无限累计)。
     * 故补 NULL 对 NULL 配对:进池的归该规则的「进池」这一桶,各专属商户各自一桶 —— 专属侧逐分不变,
     * 用户 2026-07-21 拍板 A「按费率规则各自封、全局规则各商户各自封顶」不受影响。
     * 两个分支都走 {@code idx_st_svcfee_mtd}(等值 / IS NULL 都可索引),不改索引。
     */
    public long sumMonthToDateServiceFee(String exclusiveMerchantId, String configId,
                                         Instant monthStart, Instant nextMonthStart) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(comp_service_fee_cent), 0)
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND (exclusive_merchant_id = :merchant_id
                        OR (:merchant_id::text IS NULL AND exclusive_merchant_id IS NULL))
                   AND comp_service_fee_config_id = :config_id
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                """, new MapSqlParameterSource()
                .addValue("merchant_id", exclusiveMerchantId)
                .addValue("config_id", configId)
                .addValue("start", Timestamp.from(monthStart))
                .addValue("end", Timestamp.from(nextMonthStart)), Long.class);
        return sum == null ? 0L : sum;
    }

    /**
     * S3「一张票只收一道门票综合服务费」判定:该父票<b>迄今</b>已落的综合服务费净额(核销正 + 冲正负)。
     *
     * <p>业务上回答的是一句话:<b>这张票收过这道费了吗</b>。收过(净额非零)→ 本次核销不再收;
     * 若那笔核销后来被冲掉,冲正行取原行之反把净额抵回 0 → <b>锚点自动复位</b>,这张票下一次核销可以重新收一道
     * (票还在、费该收,不能因为退过一次核销就永远免单)。
     *
     * <p>不切月窗:「一张票一道」是<b>票的属性</b>,不是月的属性 —— 跨月多次核销的票只该收一道,
     * 与月度封顶({@link #sumMonthToDateServiceFee},按「商户×规则」按月)是两件事,两道闸并行不冲突。
     */
    public long sumTicketServiceFeeCent(String parentTicketId) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(comp_service_fee_cent), 0)
                  FROM settlement_transactions
                 WHERE parent_ticket_id = :parent_ticket_id
                   AND transaction_type IN ('verification_settled','verification_reversed')
                """, new MapSqlParameterSource().addValue("parent_ticket_id", parentTicketId), Long.class);
        return sum == null ? 0L : sum;
    }

    /**
     * S6「一张票只计提一道赠送权益预估成本」判定:该父票<b>迄今</b>已落的 G 净额(核销正 + 冲正负)。
     *
     * <p>与 {@link #sumTicketServiceFeeCent} 同一范式:净额非零 = 这张票计提过了 → 后续核销不再计提;
     * 冲正取原行之反把净额抵回 0 → 锚点自动复位,该票下一次核销可重新计提(票还在、成本该预扣)。
     * 不切月窗:「一张票一道」是票的属性,不是月的属性。
     */
    public long sumTicketGiftCostCent(String parentTicketId) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(gift_cost_cent), 0)
                  FROM settlement_transactions
                 WHERE parent_ticket_id = :parent_ticket_id
                   AND transaction_type IN ('verification_settled','verification_reversed')
                """, new MapSqlParameterSource().addValue("parent_ticket_id", parentTicketId), Long.class);
        return sum == null ? 0L : sum;
    }

    /**
     * S6 增量封顶:某「项目 × G 规则」在 [monthStart,nextMonthStart) 内已落的 G 累计
     * (核销正 + 冲正负 → 冲正天然释放封顶额度)。与 {@link #sumMonthToDateServiceFee} 同范式,
     * 分组键换「项目 × 规则」(V372 拍定):G 归娱乐平台公司,封顶管的是"这个项目这条规则当月最多预扣多少",
     * 与商户无关。project NULL 对 NULL 配对(平台默认口径自成一桶)。
     */
    public long sumMonthToDateGiftCost(String projectId, String configId,
                                       Instant monthStart, Instant nextMonthStart) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(gift_cost_cent), 0)
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND (project_id = :project_id OR (:project_id::text IS NULL AND project_id IS NULL))
                   AND gift_cost_config_id = :config_id
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("config_id", configId)
                .addValue("start", Timestamp.from(monthStart))
                .addValue("end", Timestamp.from(nextMonthStart)), Long.class);
        return sum == null ? 0L : sum;
    }

    private MapSqlParameterSource monthWindow(String projectId, Instant monthStart, Instant nextMonthStart) {
        return monthWindow(projectId, monthStart, nextMonthStart, null);
    }

    private MapSqlParameterSource monthWindow(String projectId, Instant monthStart, Instant nextMonthStart,
                                              Instant recoveryCutoff) {
        return new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("start", Timestamp.from(monthStart))
                .addValue("end", Timestamp.from(nextMonthStart))
                .addValue("recovery_cutoff", recoveryCutoff == null ? null : Timestamp.from(recoveryCutoff),
                        java.sql.Types.TIMESTAMP);
    }

    /**
     * 第十八批 AI 段 214-R2（ENUM:49「其一，到期未核销票款」「游乐域 → 游乐公司」；ENUM:48「按本表「域归属对照」条归属」「仅扣实际发生的通路费」
     * 「各项均只扣一次」；第十八批裁决 51「从未核销、到期的部分在到期所在月另挂一笔」）：本月到期未核销事实中归游乐、传媒、餐饮、零售业态归属主体的
     * 票款，按业态汇总未核销额减到期作业已扣的通路费与销售渠道费（单一经营域尾差归该域主体时一并扣）；两类到期事实（从未检票、场次终结）都并入。
     * 归属主体自收的纯游乐票、纯游乐通票与传媒自办演出票（{@link #OWN_COLLECTED}）、酒吧（娱乐平台公司自身）与「其他」不进。
     */
    public java.util.Map<String, Long> aggregateUnverifiedNetByFormat(String projectId, Instant monthStart, Instant nextMonthStart) {
        java.util.Map<String, Long> out = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT COALESCE(m.merchant_type, 'bar') AS business_format,
                       COALESCE(SUM(st.unverified_amount_cent - st.route_fee_cent - st.channel_fee_cent
                                    - CASE WHEN st.channel_fee_residual_entity_id = st.beneficiary_entity_id
                                           THEN st.channel_fee_residual_cent ELSE 0 END), 0) AS net_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                 WHERE st.transaction_type = 'unverified_expired'
                   AND COALESCE(st.business_at, st.created_at) >= :start AND COALESCE(st.business_at, st.created_at) < :end
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                   AND COALESCE(m.merchant_type, 'bar') IN ('amusement','media','catering','retail')
                   AND NOT (COALESCE(m.merchant_type, 'bar') IN ('amusement','media') AND """
                + " " + OWN_COLLECTED.formatted("st", "COALESCE(m.merchant_type, 'bar')") + """
                )
                 GROUP BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart), rs -> {
                    out.put(rs.getString("business_format"), rs.getLong("net_cent"));
                });
        return out;
    }

    /** FS-021-R2：本窗内被撤销、早于本窗的原核销业务时刻，升序；第十八批 X 段 189-R1：原月由调用方按营业日定。 */
    public List<Instant> findRecoveredOriginalTimes(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT COALESCE(o.business_at, o.created_at)
                  FROM settlement_transactions st
                  JOIN settlement_transactions o ON o.transaction_no = 'ST-VS-' || st.verification_id
                 WHERE st.transaction_type = 'verification_reversed'
                   AND COALESCE(st.business_at, st.created_at) >= :start AND COALESCE(st.business_at, st.created_at) < :end
                   AND COALESCE(o.business_at, o.created_at) < :start
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart), Instant.class);
    }

    /** FS-021-R2：追收明细按原月结算单拆分的金额与回指。 */
    public void insertItemRecovery(String itemId, String originalBillId, long amountCent) {
        jdbcTemplate.update("""
                INSERT INTO settlement_item_recoveries (item_id, original_bill_id, amount_cent)
                VALUES (:item_id, :original_bill_id, :amount_cent)
                """, new MapSqlParameterSource().addValue("item_id", itemId).addValue("original_bill_id", originalBillId)
                .addValue("amount_cent", amountCent));
    }

    // ---- S7 赠付差额结算(HTML:962「预扣与核销实付的差额留在娱乐平台公司,按可配规则调账留痕」)----

    /**
     * S7 当月<b>预扣 G 按规则</b>聚合:与 {@link #sumPooledTotals} 完全相同的进池过滤,只对
     * gift_cost_config_id 非空的行分组(计提/冲正行才带规则 id;非计提行 G 恒 0,不进和也不丢钱)。
     * 引擎用「Σ各规则 G ≡ 进池 G 合计」互核——若有 G 落库却没带规则 id,月结当场响亮停,
     * 而不是差额表悄悄少一块、财务月底对不上。
     */
    public List<ConfigGiftCost> aggregateGiftCostByConfig(String projectId, Instant monthStart, Instant nextMonthStart,
                                                          Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT gift_cost_config_id                    AS config_id,
                       COALESCE(SUM(gift_cost_cent), 0)       AS gift_cost_cent
                  FROM settlement_transactions st
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND verification_id IS NOT NULL
                   AND exclusive_merchant_id IS NULL
                   AND gift_cost_config_id IS NOT NULL
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new ConfigGiftCost(rs.getString("config_id"), rs.getLong("gift_cost_cent")));
    }

    /**
     * S7 当月<b>兑付实付 A</b> 按(票种 × 提供方业态)聚合:
     * A = ar_ap_items(source_type='pass_drink_settlement') 的应付净额(payable 为正、对冲 receivable 为负,
     * V344 红冲行金额自带符号随 direction 归并)。月窗/项目/票种锚点取该核销在
     * settlement_transactions 里的结算事实行——应付联原核销行(verification_settled)、
     * 对冲联冲正行(verification_reversed):跨月红冲的对冲与 G 的冲正行落<b>同一个月</b>,K12 不被跨月错位打破。
     * 业态 = 提供方商户(应付对手方)的 merchant_type,与 {@link #aggregateFormatRevenue} 同一取法
     * (解析不到按酒吧计,同一 COALESCE 口径,不静默丢钱)。
     * 票种维度供引擎按「项目 × 票种」解析差额规则(与 G 计提用同一个 {@link #findPassGiftCostConfig})。
     *
     * <p>ponytail: 核销无结算事实行(direct 出票无分摊快照被消费者跳过)时其兑付不进本聚合——
     * 该场景无月窗/项目锚点可依;真出现由结算重建任务补建事实行后自然并入。
     *
     * <p>第十八批 AI 段 216-R1（ENUM:52「按件计＝票面各权益数量乘提供方内部结算价之和……该笔预扣归娱乐平台公司，预扣数与日后实付数的差额
     * 也留在娱乐平台公司」；BSM:752 同句）：另含零元专属直付（平台出资的餐饮、零售赠送，不记域份额）按内部结算价直付的核销与冲正额，
     * 业态取专属商户；有价专属直付行（记了域份额）与单一酒吧门票行不进。商户让利的零元行结算价为 0，不影响。
     */
    public List<GiftPaid> aggregateGiftPaid(String projectId, Instant monthStart, Instant nextMonthStart,
                                            Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT ticket_type_id, business_format, COALESCE(SUM(paid_cent), 0) AS paid_cent FROM (
                SELECT st.ticket_type_id,
                       COALESCE(m.merchant_type, 'bar')       AS business_format,
                       CASE WHEN a.direction = 'payable' THEN a.amount_cent ELSE -a.amount_cent END AS paid_cent
                  FROM ar_ap_items a
                  JOIN settlement_transactions st
                    ON st.verification_id = a.source_id
                   AND st.transaction_type = CASE a.direction WHEN 'payable' THEN 'verification_settled'
                                                              ELSE 'verification_reversed' END
                  LEFT JOIN merchants m ON m.id = a.counterparty_id
                 WHERE a.source_type = 'pass_drink_settlement'
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 UNION ALL
                SELECT st.ticket_type_id, COALESCE(m.merchant_type, 'bar'), st.verified_amount_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.exclusive_merchant_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NOT NULL
                   AND st.domain_share_cent IS NULL
                """ + "   AND NOT " + SINGLE_BAR_ROW.formatted("st") + "\n" + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                ) paid
                 GROUP BY 1, 2
                 ORDER BY 1, 2
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new GiftPaid(rs.getString("ticket_type_id"), rs.getString("business_format"),
                        rs.getLong("paid_cent")));
    }

    /**
     * 第十八批 AI 段 237-R2（BSM:1754「按项目、经营域、责任主体和实际提供方列明……内部结算价权益」）：月窗内按实际提供方汇总内部结算价权益——
     * 赠饮兑付应付净额（联结与月窗同 {@link #aggregateGiftPaid} 第一支，按应付对手方分组），与通票专属直付按内部结算价的直付额：有价行（记了域份额，
     * 差额由该域归属主体承担）与零元平台出资行（娱乐平台公司承担）分两列。单一酒吧门票行是门票票款、不是内部结算价权益，不进。
     */
    public List<ProviderInternalPrice> aggregateInternalPriceByProvider(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.query("""
                SELECT p.merchant_id, m.merchant_name, COALESCE(m.merchant_type, 'bar') AS business_format,
                       COALESCE(SUM(p.gift_cent), 0) AS gift_drink_paid_cent,
                       COALESCE(SUM(p.paid_cent), 0) AS paid_direct_settled_cent,
                       COALESCE(SUM(p.free_cent), 0) AS free_direct_settled_cent
                  FROM (
                SELECT a.counterparty_id AS merchant_id,
                       CASE WHEN a.direction = 'payable' THEN a.amount_cent ELSE -a.amount_cent END AS gift_cent,
                       0 AS paid_cent, 0 AS free_cent
                  FROM ar_ap_items a
                  JOIN settlement_transactions st
                    ON st.verification_id = a.source_id
                   AND st.transaction_type = CASE a.direction WHEN 'payable' THEN 'verification_settled'
                                                              ELSE 'verification_reversed' END
                 WHERE a.source_type = 'pass_drink_settlement'
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 UNION ALL
                SELECT st.exclusive_merchant_id, 0,
                       CASE WHEN st.domain_share_cent IS NOT NULL THEN st.verified_amount_cent ELSE 0 END,
                       CASE WHEN st.domain_share_cent IS NULL THEN st.verified_amount_cent ELSE 0 END
                  FROM settlement_transactions st
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NOT NULL
                """ + "   AND NOT " + SINGLE_BAR_ROW.formatted("st") + "\n" + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                ) p
                  LEFT JOIN merchants m ON m.id = p.merchant_id
                 GROUP BY 1, 2, 3
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) -> new ProviderInternalPrice(
                rs.getString("merchant_id"), rs.getString("merchant_name"), rs.getString("business_format"),
                rs.getLong("gift_drink_paid_cent"), rs.getLong("paid_direct_settled_cent"), rs.getLong("free_direct_settled_cent")));
    }

    /**
     * S7 按规则 id 读差额处理规则(快照读:不过滤 status/is_draft——G 计提行引用的规则可能已被
     * 软替换/软删,差额结算仍须按当时那条规则的口径处理;软删铁律保证行永在,查无此行=数据被物理
     * 删=响亮停)。
     */
    public String findVarianceHandling(String configId) {
        String handling = jdbcTemplate.query(
                "SELECT variance_handling FROM pass_gift_cost_configs WHERE id = :id",
                new MapSqlParameterSource().addValue("id", configId),
                rs -> rs.next() ? rs.getString("variance_handling") : null);
        return requireConfigured(handling, "pass_gift_cost_configs.variance_handling / id=" + configId, null);
    }

    /** S7 本单台账现状:按规则聚合净额(append-only 全行相加,冲正行自带负号) + 最新非冲正行 id(供冲正指认)。 */
    public java.util.Map<String, VarianceNet> findVarianceNetByBill(String billId) {
        java.util.Map<String, VarianceNet> out = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT config_id,
                       COALESCE(SUM(accrued_cost_cent), 0)    AS accrued,
                       COALESCE(SUM(actual_paid_cent), 0)     AS paid,
                       MAX(id) FILTER (WHERE reversal_of IS NULL) AS latest_statement_id
                  FROM pass_gift_cost_variance_ledger
                 WHERE bill_id = :bill_id
                 GROUP BY config_id
                """, new MapSqlParameterSource().addValue("bill_id", billId), rs -> {
            out.put(rs.getString("config_id"), new VarianceNet(
                    rs.getLong("accrued"), rs.getLong("paid"), rs.getString("latest_statement_id")));
        });
        return out;
    }

    /** S7 追加一行差额台账(append-only,只 INSERT;更正由调用方追加冲正行,绝不 UPDATE)。 */
    public void insertVarianceRow(VarianceLedgerRow row) {
        jdbcTemplate.update("""
                INSERT INTO pass_gift_cost_variance_ledger (
                    id, bill_id, project_id, period_start, period_end, config_id, variance_handling,
                    accrued_cost_cent, actual_paid_cent, variance_cent,
                    beneficiary_entity_id, reversal_of)
                VALUES (:id, :bill_id, :project_id, :period_start, :period_end, :config_id, :variance_handling,
                        :accrued, :paid, :variance, :beneficiary_entity_id, :reversal_of)
                """, new MapSqlParameterSource()
                .addValue("id", row.id())
                .addValue("bill_id", row.billId())
                .addValue("project_id", row.projectId())
                .addValue("period_start", java.sql.Date.valueOf(row.periodStart()))
                .addValue("period_end", java.sql.Date.valueOf(row.periodEnd()))
                .addValue("config_id", row.configId())
                .addValue("variance_handling", row.varianceHandling())
                .addValue("accrued", row.accruedCostCent())
                .addValue("paid", row.actualPaidCent())
                .addValue("variance", row.varianceCent())
                .addValue("beneficiary_entity_id", row.beneficiaryEntityId())
                .addValue("reversal_of", row.reversalOf()));
    }

    /** S7 当月某规则的 G 聚合行(预扣合计,带符号)。 */
    public record ConfigGiftCost(String configId, long giftCostCent) {
    }

    /** S7 当月(票种 × 提供方业态)的兑付实付净额(带符号;票种供按「项目×票种」解析差额规则)。 */
    public record GiftPaid(String ticketTypeId, String businessFormat, long paidCent) {
    }

    /** S7 本单台账某规则的净额现状(append-only 聚合;latestStatementId 供冲正行回指)。 */
    public record VarianceNet(long accruedCostCent, long actualPaidCent, String latestStatementId) {

        public long varianceCent() {
            return accruedCostCent - actualPaidCent;
        }

        boolean isZero() {
            return accruedCostCent == 0L && actualPaidCent == 0L;
        }
    }

    /** S7 差额台账一行(冲正行=全金额取反 + reversalOf 指向被冲行)。 */
    public record VarianceLedgerRow(String id, String billId, String projectId,
                                    java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
                                    String configId, String varianceHandling,
                                    long accruedCostCent, long actualPaidCent, long varianceCent,
                                    String beneficiaryEntityId, String reversalOf) {
    }

    /**
     * 门票综合服务费费率规则(= 娱乐平台公司留存 / 项目管理费):按【项目 × 商户 × 票种】范围可配,默认 0(不抽)。
     * 查询精度:商户+票种特例 &gt; 商户特例 &gt; 票种特例 &gt; 项目默认(留空=该层通用,ORDER 让更具体的优先)。
     * 无任何配置行 -&gt; {@link #requireConfigured} 响亮停(禁硬编码回退)。
     *
     * <p><b>S2(2026-07-28,V365)第三维换成票种</b>:原来是权益类型(bar/amusement/…),与 HTML
     * 「可按（项目 × 商户 × 票种）范围……」不符。业务上按票种谈费率、按票种对账,一张通票里两类权益
     * 按两套费率各抽一道时,业务人员在结算单上回答不了「这张票收了多少综合服务费」。
     *
     * <p><b>项目维度改用 {@code (project_id IS NULL OR project_id = :project_id)}</b>:原 COALESCE 哨兵写法
     * 会把真实 project_id 恰为 '__DEFAULT__' 的行对所有项目可见(跨租户读);S2 起调用处开始传真实 project scope,
     * 该风险不再是理论问题。同 {@code JdbcCommissionPointsConfigRepository} 2026-07-28 修 verify:tenant-isolation F1 的口径。
     */
    public ServiceFeeConfig findComprehensiveServiceFee(String projectId, String merchantId, String ticketTypeId) {
        ServiceFeeConfig cfg = jdbcTemplate.query("""
                SELECT id, fee_bps, fixed_amount_cent, monthly_cap_amount_cent FROM comprehensive_service_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND (project_id IS NULL OR project_id = :project_id)
                   AND (merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (ticket_type_id IS NULL OR ticket_type_id = :ticket_type_id)
                 ORDER BY (project_id IS NOT NULL) DESC,
                          (merchant_id IS NOT NULL) DESC,
                          (ticket_type_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId)
                .addValue("ticket_type_id", ticketTypeId),
                rs -> rs.next()
                        ? new ServiceFeeConfig(rs.getString("id"), rs.getInt("fee_bps"),
                                rs.getObject("fixed_amount_cent") == null ? null : rs.getLong("fixed_amount_cent"),
                                rs.getObject("monthly_cap_amount_cent") == null ? null
                                        : rs.getLong("monthly_cap_amount_cent"))
                        : null);
        return requireConfigured(cfg, "comprehensive_service_fee_configs", projectId);
    }

    /**
     * S6 赠送权益预扣规则(pass_gift_cost_configs):按【项目 × 票种】范围可配,两模式二选一
     * (by_item 按件计=票面推导、无需配金额;by_ticket_amount 按票计=票种赠送部分售价整额)。
     * 查询精度:项目+票种特例 &gt; 项目通用 &gt; 平台票种特例 &gt; 平台默认(ORDER 让更具体的优先);
     * 读侧只认生效行(is_draft=FALSE)。无任何配置行 → {@link #requireConfigured} 响亮停
     * (V373 种子 pgc_default=by_ticket_amount+0 恒在,正常不触发;种子被删=部署坏,该停)。
     * 项目维度与 {@link #findComprehensiveServiceFee} 同口径用 IS NULL OR =(不用 COALESCE 哨兵,防跨租户读)。
     */
    public PassGiftCostConfig findPassGiftCostConfig(String projectId, String ticketTypeId) {
        PassGiftCostConfig cfg = jdbcTemplate.query("""
                SELECT id, prededuct_mode, ticket_gift_amount_cent, monthly_cap_amount_cent
                  FROM pass_gift_cost_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND (project_id IS NULL OR project_id = :project_id)
                   AND (ticket_type_id IS NULL OR ticket_type_id = :ticket_type_id)
                 ORDER BY (project_id IS NOT NULL) DESC,
                          (ticket_type_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("ticket_type_id", ticketTypeId),
                rs -> rs.next()
                        ? new PassGiftCostConfig(rs.getString("id"), rs.getString("prededuct_mode"),
                                rs.getObject("ticket_gift_amount_cent") == null ? null
                                        : rs.getLong("ticket_gift_amount_cent"),
                                rs.getObject("monthly_cap_amount_cent") == null ? null
                                        : rs.getLong("monthly_cap_amount_cent"))
                        : null);
        return requireConfigured(cfg, "pass_gift_cost_configs", projectId);
    }

    /**
     * V268 套票折扣归属(§7.4 口径10):项目级可配,默认 platform_absorb(项目承担)。
     * 说明:当前无折扣额进结算引擎(折扣是定价期概念),本解析器先落地供后台/引擎读,逐笔应用待折扣额接入流水后启用。
     */
    public ComboDiscountConfig findComboDiscountConfig(String projectId) {
        ComboDiscountConfig cfg = jdbcTemplate.query("""
                SELECT discount_bearer, split_platform_bps, rounding_tolerance_cent FROM ticket_combo_discount_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                rs -> rs.next()
                        ? new ComboDiscountConfig(rs.getString("discount_bearer"), rs.getInt("split_platform_bps"),
                                rs.getLong("rounding_tolerance_cent"))
                        : null);
        return requireConfigured(cfg, "ticket_combo_discount_configs", projectId);
    }

    /**
     * V369 凑整允许误差(分),实现票务域声明的 {@link com.complex.platform.ticketing.ComboPriceToleranceConfigPort}。
     * 与折扣归属同一行配置、同一套草案转正闸;缺配置时沿用 requireConfigured 的 fail-loud(不静默给 0)。
     */
    @Override
    public long findRoundingToleranceCent(String projectId) {
        return findComboDiscountConfig(projectId).roundingToleranceCent();
    }

    /**
     * §4 回退常量收敛为 fail-loud:结算配置缺失(连 __DEFAULT__ 种子都没有=部署坏)时【响亮停】而非静默套硬编码默认。
     * 正常态 V223/V268/V327 恒建 __DEFAULT__ 行 + V326 已转正 is_draft=FALSE,故 5 个 finder 恒命中,本助手不触发,零行为变更;
     * null 只在种子被删/迁移未跑到位时出现——此时报错正是「零硬编码·无配置即响亮停」铁则要的。
     * (findActiveMerchantRateRules/findRankTiers 不收敛:前者空名单=无第一段酒吧合法,后者空梯子=第二段无人可分,均引擎自处置。)
     */
    private <T> T requireConfigured(T value, String key, String projectId) {
        if (value == null) {
            throw ApiException.badRequest("SETTLEMENT_CONFIG_MISSING",
                    "结算配置缺失且无默认种子:" + key + (projectId == null ? "" : " / project=" + projectId)
                            + ";请先在结算配置中心配置并转正");
        }
        return value;
    }

    // ---- upsert 结算单头 + 明细（COALESCE(project_id)+period 唯一，幂等覆盖）----

    /** upsert settlement_bills（同项目×月幂等覆盖），返回 bill id。 */
    public void upsertBill(SettlementBillRecord bill) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", bill.id())
                .addValue("project_id", bill.projectId())
                .addValue("period_start", java.sql.Date.valueOf(bill.periodStart()))
                .addValue("period_end", java.sql.Date.valueOf(bill.periodEnd()))
                .addValue("ticket_revenue_cent", bill.ticketRevenueCent())
                .addValue("channel_fee_cent", bill.channelFeeCent())
                .addValue("route_fee_cent", bill.routeFeeCent())
                .addValue("available_pool_cent", bill.availablePoolCent())
                .addValue("distributed_cent", bill.distributedCent())
                .addValue("comp_service_fee_pooled_cent", bill.compServiceFeePooledCent())
                .addValue("comp_service_fee_exclusive_cent", bill.compServiceFeeExclusiveCent())
                .addValue("fee_beneficiary_entity_id", bill.feeBeneficiaryEntityId())
                .addValue("gift_cost_cent", bill.giftCostCent())
                .addValue("gift_beneficiary_entity_id", bill.giftBeneficiaryEntityId())
                .addValue("distribution_method", bill.distributionMethod())
                .addValue("status", bill.status())
                .addValue("attributed_entity_id", bill.attributedEntityId())
                .addValue("combo_discount_cent", bill.comboDiscountCent())
                .addValue("rounding_residual_cent", bill.roundingResidualCent())
                .addValue("rounding_residual_entity_id", bill.roundingResidualEntityId())
                .addValue("pool_undistributed_cent", bill.poolUndistributedCent())
                .addValue("pool_undistributed_entity_id", bill.poolUndistributedEntityId())
                .addValue("unverified_deduction_cent", bill.unverifiedDeductionCent())
                .addValue("unverified_deduction_entity_id", bill.unverifiedDeductionEntityId())
                .addValue("pool_suspended_cent", bill.poolSuspendedCent())
                .addValue("pool_overflow_cent", bill.poolOverflowCent());
        jdbcTemplate.update("""
                INSERT INTO settlement_bills (
                    id, project_id, period_start, period_end, ticket_revenue_cent, channel_fee_cent, route_fee_cent,
                    available_pool_cent, distributed_cent,
                    comp_service_fee_pooled_cent, comp_service_fee_exclusive_cent, fee_beneficiary_entity_id,
                    gift_cost_cent, gift_beneficiary_entity_id,
                    distribution_method, status,
                    attributed_entity_id, combo_discount_cent, rounding_residual_cent,
                    rounding_residual_entity_id,
                    pool_undistributed_cent, pool_undistributed_entity_id,
                    unverified_deduction_cent, unverified_deduction_entity_id, pool_suspended_cent, pool_overflow_cent,
                    updated_at
                )
                VALUES (
                    :id, :project_id, :period_start, :period_end, :ticket_revenue_cent, :channel_fee_cent, :route_fee_cent,
                    :available_pool_cent, :distributed_cent,
                    :comp_service_fee_pooled_cent, :comp_service_fee_exclusive_cent, :fee_beneficiary_entity_id,
                    :gift_cost_cent, :gift_beneficiary_entity_id,
                    :distribution_method, :status,
                    :attributed_entity_id, :combo_discount_cent, :rounding_residual_cent,
                    :rounding_residual_entity_id,
                    :pool_undistributed_cent, :pool_undistributed_entity_id,
                    :unverified_deduction_cent, :unverified_deduction_entity_id, :pool_suspended_cent, :pool_overflow_cent,
                    now()
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), period_start, period_end) DO UPDATE SET
                    ticket_revenue_cent = EXCLUDED.ticket_revenue_cent,
                    channel_fee_cent    = EXCLUDED.channel_fee_cent,
                    route_fee_cent      = EXCLUDED.route_fee_cent,
                    available_pool_cent = EXCLUDED.available_pool_cent,
                    distributed_cent    = EXCLUDED.distributed_cent,
                    comp_service_fee_pooled_cent    = EXCLUDED.comp_service_fee_pooled_cent,
                    comp_service_fee_exclusive_cent = EXCLUDED.comp_service_fee_exclusive_cent,
                    fee_beneficiary_entity_id       = EXCLUDED.fee_beneficiary_entity_id,
                    gift_cost_cent                  = EXCLUDED.gift_cost_cent,
                    gift_beneficiary_entity_id      = EXCLUDED.gift_beneficiary_entity_id,
                    distribution_method = EXCLUDED.distribution_method,
                    status              = EXCLUDED.status,
                    attributed_entity_id = EXCLUDED.attributed_entity_id,
                    combo_discount_cent = EXCLUDED.combo_discount_cent,
                    rounding_residual_cent = EXCLUDED.rounding_residual_cent,
                    rounding_residual_entity_id = EXCLUDED.rounding_residual_entity_id,
                    pool_undistributed_cent      = EXCLUDED.pool_undistributed_cent,
                    pool_undistributed_entity_id = EXCLUDED.pool_undistributed_entity_id,
                    unverified_deduction_cent      = EXCLUDED.unverified_deduction_cent,
                    unverified_deduction_entity_id = EXCLUDED.unverified_deduction_entity_id,
                    pool_suspended_cent            = EXCLUDED.pool_suspended_cent,
                    pool_overflow_cent             = EXCLUDED.pool_overflow_cent,
                    updated_at          = now()
                """, params);
    }

    /** 取回结算单实际落地 id（幂等覆盖时 EXCLUDED.id 不生效，须按唯一键回查真身）。 */
    public String findBillId(String projectId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
        return jdbcTemplate.queryForObject("""
                SELECT id FROM settlement_bills
                 WHERE COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                   AND period_start = :period_start AND period_end = :period_end
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period_start", java.sql.Date.valueOf(periodStart))
                .addValue("period_end", java.sql.Date.valueOf(periodEnd)),
                String.class);
    }

    /** 幂等重跑：先清掉该单旧明细，再按当月重算插入（明细集合随核销变化，覆盖式最简）。 */
    public void deleteItemsByBill(String billId) {
        jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id = :bill_id",
                new MapSqlParameterSource().addValue("bill_id", billId));
    }

    /** S11：清本单业态份额留痕（幂等重跑先清后写，与明细同范式）。 */
    public void deleteFormatSharesByBill(String billId) {
        jdbcTemplate.update("DELETE FROM settlement_bill_format_shares WHERE bill_id = :bill_id",
                new MapSqlParameterSource().addValue("bill_id", billId));
    }

    /** S11：写一行结算单 × 业态份额留痕（V366;S4 补本业态综合服务费列;S6 补本业态赠送权益预估成本列;
     *  S7 补本业态兑付实付列 gift_actual_paid_cent——纯留痕不进净份额；第十八批 AI 段 V622 补域间份额、追收与到期未核销归属四列）。 */
    public void upsertFormatShare(SettlementBillFormatShareRecord share) {
        jdbcTemplate.update("""
                INSERT INTO settlement_bill_format_shares (
                    bill_id, business_format, gross_revenue_cent, platform_borne_discount_cent,
                    channel_fee_cent, route_fee_cent, comp_service_fee_cent, gift_cost_cent, gift_actual_paid_cent,
                    net_revenue_cent, beneficiary_kind, beneficiary_entity_id, internal_settled_cent,
                    inter_entity_net_cent, recovery_net_cent, recovery_internal_settled_cent, unverified_net_cent)
                VALUES (:bill_id, :business_format, :gross, :discount, :channel_fee, :route_fee, :comp_service_fee,
                        :gift_cost, :gift_actual_paid, :net, :beneficiary_kind, :beneficiary_entity_id, :internal_settled,
                        :inter_entity_net, :recovery_net, :recovery_internal_settled, :unverified_net)
                ON CONFLICT (bill_id, business_format) DO UPDATE SET
                    gross_revenue_cent           = EXCLUDED.gross_revenue_cent,
                    platform_borne_discount_cent = EXCLUDED.platform_borne_discount_cent,
                    channel_fee_cent             = EXCLUDED.channel_fee_cent,
                    route_fee_cent               = EXCLUDED.route_fee_cent,
                    comp_service_fee_cent        = EXCLUDED.comp_service_fee_cent,
                    gift_cost_cent               = EXCLUDED.gift_cost_cent,
                    gift_actual_paid_cent        = EXCLUDED.gift_actual_paid_cent,
                    net_revenue_cent             = EXCLUDED.net_revenue_cent,
                    beneficiary_kind             = EXCLUDED.beneficiary_kind,
                    beneficiary_entity_id        = EXCLUDED.beneficiary_entity_id,
                    internal_settled_cent        = EXCLUDED.internal_settled_cent,
                    inter_entity_net_cent        = EXCLUDED.inter_entity_net_cent,
                    recovery_net_cent            = EXCLUDED.recovery_net_cent,
                    recovery_internal_settled_cent = EXCLUDED.recovery_internal_settled_cent,
                    unverified_net_cent          = EXCLUDED.unverified_net_cent,
                    updated_at                   = now()
                """, new MapSqlParameterSource()
                .addValue("bill_id", share.billId())
                .addValue("business_format", share.businessFormat())
                .addValue("gross", share.grossRevenueCent())
                .addValue("discount", share.platformBorneDiscountCent())
                .addValue("channel_fee", share.channelFeeCent())
                .addValue("route_fee", share.routeFeeCent())
                .addValue("comp_service_fee", share.compServiceFeeCent())
                .addValue("gift_cost", share.giftCostCent())
                .addValue("gift_actual_paid", share.giftActualPaidCent())
                .addValue("net", share.netRevenueCent())
                .addValue("beneficiary_kind", share.beneficiaryKind())
                .addValue("beneficiary_entity_id", share.beneficiaryEntityId())
                .addValue("internal_settled", share.internalSettledCent())
                .addValue("inter_entity_net", share.interEntityNetCent())
                .addValue("recovery_net", share.recoveryNetCent())
                .addValue("recovery_internal_settled", share.recoveryInternalSettledCent())
                .addValue("unverified_net", share.unverifiedNetCent()));
    }

    /** 第十八批 AI 段 214-R1、214-R2：过账读本单各业态份额行（域间份额、代付、追收、到期未核销归属与归属主体）。 */
    public List<SettlementBillFormatShareRecord> listFormatSharesForPosting(String billId) {
        return jdbcTemplate.query("SELECT * FROM settlement_bill_format_shares WHERE bill_id = :bill_id ORDER BY business_format",
                new MapSqlParameterSource("bill_id", billId), (rs, i) -> new SettlementBillFormatShareRecord(
                        rs.getString("bill_id"), rs.getString("business_format"), rs.getLong("gross_revenue_cent"),
                        rs.getLong("platform_borne_discount_cent"), rs.getLong("channel_fee_cent"), rs.getLong("route_fee_cent"),
                        rs.getLong("comp_service_fee_cent"), rs.getLong("gift_cost_cent"), rs.getLong("gift_actual_paid_cent"),
                        rs.getLong("net_revenue_cent"), rs.getString("beneficiary_kind"), rs.getString("beneficiary_entity_id"),
                        rs.getLong("internal_settled_cent"), rs.getLong("inter_entity_net_cent"), rs.getLong("recovery_net_cent"),
                        rs.getLong("recovery_internal_settled_cent"), rs.getLong("unverified_net_cent")));
    }

    /**
     * 第十八批 AI 段 237-R2（BSM:1754「按项目、经营域、责任主体和实际提供方列明第三方渠道费用、域间份额……和责任主体承担的差额」；ENUM:53「自行承担差额」）：
     * 审阅明细读本单各业态份额行与责任主体名；差额＝净份额−按内部结算价直付额，只对有直付的行列。
     */
    public List<FormatShareView> listFormatShareViews(String billId) {
        return jdbcTemplate.query("""
                SELECT s.*, be.entity_name AS beneficiary_entity_name
                  FROM settlement_bill_format_shares s
                  LEFT JOIN business_entities be ON be.id = s.beneficiary_entity_id
                 WHERE s.bill_id = :bill_id
                 ORDER BY s.business_format
                """, new MapSqlParameterSource("bill_id", billId), (rs, i) -> {
                    long net = rs.getLong("net_revenue_cent");
                    long internal = rs.getLong("internal_settled_cent");
                    return new FormatShareView(rs.getString("business_format"), rs.getString("beneficiary_kind"),
                            rs.getString("beneficiary_entity_id"), rs.getString("beneficiary_entity_name"),
                            rs.getLong("gross_revenue_cent"), rs.getLong("channel_fee_cent"), rs.getLong("route_fee_cent"),
                            rs.getLong("comp_service_fee_cent"), rs.getLong("gift_cost_cent"), rs.getLong("gift_actual_paid_cent"),
                            net, rs.getLong("inter_entity_net_cent"), rs.getLong("unverified_net_cent"), internal,
                            internal == 0L ? null : net - internal);
                });
    }

    /** 第十八批 AI 段 237-R2（BSM:1754「按……实际提供方列明……域内分配」）：审阅明细读本单各商户分账明细（连商户名、业态与单一酒吧门票两层费）。 */
    public List<ItemView> listItemViews(String billId) {
        return jdbcTemplate.query("""
                SELECT si.merchant_id, m.merchant_name, COALESCE(m.merchant_type, 'bar') AS business_format,
                       si.distribution_method, si.verified_count, si.guest_count, si.rank_no, si.final_amount_cent,
                       si.single_bar_route_fee_cent, si.single_bar_channel_fee_cent, si.status
                  FROM settlement_items si
                  LEFT JOIN merchants m ON m.id = si.merchant_id
                 WHERE si.bill_id = :bill_id
                 ORDER BY si.distribution_method, si.rank_no NULLS LAST, si.merchant_id
                """, new MapSqlParameterSource("bill_id", billId), (rs, i) -> new ItemView(
                rs.getString("merchant_id"), rs.getString("merchant_name"), rs.getString("business_format"),
                rs.getString("distribution_method"), rs.getInt("verified_count"), rs.getInt("guest_count"),
                rs.getObject("rank_no", Integer.class), rs.getLong("final_amount_cent"), rs.getLong("single_bar_route_fee_cent"),
                rs.getLong("single_bar_channel_fee_cent"), rs.getString("status")));
    }

    /** upsert settlement_items（bill+merchant 唯一）。 */
    public void upsertItem(SettlementItemRecord item) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("bill_id", item.billId())
                .addValue("merchant_id", item.merchantId())
                .addValue("verified_count", item.verifiedCount())
                .addValue("guest_count", item.guestCount())
                .addValue("rank_no", item.rankNo())
                .addValue("distribution_method", item.distributionMethod())
                .addValue("calculated_amount_cent", item.calculatedAmountCent())
                .addValue("final_amount_cent", item.finalAmountCent())
                .addValue("status", item.status())
                .addValue("single_bar_route_fee_cent", item.singleBarRouteFeeCent())
                .addValue("single_bar_channel_fee_cent", item.singleBarChannelFeeCent());
        jdbcTemplate.update("""
                INSERT INTO settlement_items (
                    id, bill_id, merchant_id, verified_count, guest_count, rank_no, distribution_method,
                    calculated_amount_cent, final_amount_cent, status, single_bar_route_fee_cent, single_bar_channel_fee_cent,
                    updated_at
                )
                VALUES (
                    :id, :bill_id, :merchant_id, :verified_count, :guest_count, :rank_no, :distribution_method,
                    :calculated_amount_cent, :final_amount_cent, :status, :single_bar_route_fee_cent, :single_bar_channel_fee_cent,
                    now()
                )
                ON CONFLICT (bill_id, merchant_id, distribution_method) DO UPDATE SET
                    verified_count         = EXCLUDED.verified_count,
                    guest_count            = EXCLUDED.guest_count,
                    rank_no                = EXCLUDED.rank_no,
                    distribution_method    = EXCLUDED.distribution_method,
                    calculated_amount_cent = EXCLUDED.calculated_amount_cent,
                    final_amount_cent      = EXCLUDED.final_amount_cent,
                    status                 = EXCLUDED.status,
                    single_bar_route_fee_cent   = EXCLUDED.single_bar_route_fee_cent,
                    single_bar_channel_fee_cent = EXCLUDED.single_bar_channel_fee_cent,
                    updated_at             = now()
                """, params);
    }

    // ---- 批2-4 过账读写（供 SettlementPostingService；不改批2-3 引擎算法）----

    /** 读结算单当前 status（过账前判 confirmed）。单不存在返回 null。 */
    public String findBillStatus(String billId) {
        return jdbcTemplate.query(
                "SELECT status FROM settlement_bills WHERE id = :id",
                new MapSqlParameterSource().addValue("id", billId),
                rs -> rs.next() ? rs.getString("status") : null);
    }

    /** 按期查结算单状态(null=该期无单);月结重跑前守卫用,避免覆盖已过账/已结算单致孤儿 AR/AP。 */
    public String findBillStatusByPeriod(String projectId, java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
        return jdbcTemplate.query("""
                SELECT status FROM settlement_bills
                 WHERE COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                   AND period_start = :period_start AND period_end = :period_end
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period_start", java.sql.Date.valueOf(periodStart))
                .addValue("period_end", java.sql.Date.valueOf(periodEnd)),
                rs -> rs.next() ? rs.getString("status") : null);
    }

    /** 该单待过账明细：status='draft' 且 final_amount_cent≠0（FS-021-R1：负额=向商户追回，过账为单独追收应收；final=0 不取）。
     *  D7：带 distribution_method，供过账区分池明细是否走超额 C 口径两段拆。 */
    public List<PostableItem> findPostableItems(String billId) {
        return jdbcTemplate.query("""
                SELECT i.id, i.merchant_id, i.final_amount_cent, i.distribution_method,
                       COALESCE(b.project_id, m.project_id) AS payee_project_id
                  FROM settlement_items i
                  JOIN settlement_bills b ON b.id = i.bill_id
                  LEFT JOIN merchants m ON m.id = i.merchant_id
                 WHERE i.bill_id = :bill_id AND i.status = 'draft' AND i.final_amount_cent <> 0
                 ORDER BY i.id
                """, new MapSqlParameterSource().addValue("bill_id", billId),
                (rs, i) -> new PostableItem(rs.getString("id"), rs.getString("merchant_id"),
                        rs.getLong("final_amount_cent"), rs.getString("distribution_method"), rs.getString("payee_project_id")));
    }

    /** 回写明细 ar_ap_item_id + status='posted'（仅当仍为 draft，防并发重复过账）。返回更新行数。 */
    public int markItemPosted(String itemId, String arApItemId) {
        return jdbcTemplate.update("""
                UPDATE settlement_items
                   SET ar_ap_item_id = :ar_ap_item_id, status = 'posted', updated_at = now()
                 WHERE id = :id AND status = 'draft'
                """, new MapSqlParameterSource()
                .addValue("id", itemId).addValue("ar_ap_item_id", arApItemId));
    }

    /** 该单是否仍有未过账（draft 且 final≠0）明细；无 → 可置 settled。 */
    public boolean hasUnpostedItems(String billId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM settlement_items
                 WHERE bill_id = :bill_id AND status = 'draft' AND final_amount_cent <> 0
                """, new MapSqlParameterSource().addValue("bill_id", billId), Integer.class);
        return cnt != null && cnt > 0;
    }

    /** 置结算单 status='settled' + settled_at=now（仅当当前 confirmed，幂等：重复过账不重置已 settled 单）。 */
    public int markBillSettled(String billId) {
        return jdbcTemplate.update("""
                UPDATE settlement_bills
                   SET status = 'settled', settled_at = now(), updated_at = now()
                 WHERE id = :id AND status = 'confirmed'
                """, new MapSqlParameterSource().addValue("id", billId));
    }

    /** payeeProjectId：往来对方所属项目——项目单即本单项目；平台单（project_id 为空）取该明细商户的项目。 */
    public record PostableItem(String itemId, String merchantId, long finalAmountCent, String distributionMethod,
                               String payeeProjectId) {
    }

    // ---- D7 超额 C 口径（第 75 条：结算环节不设超池审批，超出部分过账时由娱乐平台公司承担）----

    /**
     * D7 过账用超池上下文：结算单状态 + 可分配池 availablePool + <b>应分总额</b> pooledDistributed
     * （专属直付不占池、不参与越池判定）。overPool = pooledDistributed > availablePool；单不存在返回 null。
     * 第十八批第 2 条：逐份分账后各份「第一段实分＋第二段实分＋通票未核销减除额＋未分配＋暂挂＋尾差」恒等于本份池，
     * 只有第一段合计超过本份池时多出 pool_overflow_cent，故应分总额＝可分配池＋第一段超池额，与引擎越池判定同口径。
     * S4:池就一个——available_pool_cent(酒吧净份额 − 池链综合服务费),原 spv_pool_cent 已随留存链删除。
     */
    public OverPoolContext findOverPoolContext(String billId) {
        return jdbcTemplate.query("""
                SELECT b.status, b.project_id, b.period_start, b.available_pool_cent,
                       b.available_pool_cent + b.pool_overflow_cent AS pooled_distributed_cent
                  FROM settlement_bills b
                 WHERE b.id = :id
                """, new MapSqlParameterSource().addValue("id", billId),
                rs -> rs.next()
                        ? new OverPoolContext(rs.getString("status"), rs.getString("project_id"),
                                rs.getLong("available_pool_cent"), rs.getLong("pooled_distributed_cent"),
                                rs.getObject("period_start", java.time.LocalDate.class))
                        : null);
    }

    /** D7 超池上下文读投影：overPool = pooledDistributedCent &gt; availablePoolCent。 */
    public record OverPoolContext(String status, String projectId, long availablePoolCent, long pooledDistributedCent,
                                  java.time.LocalDate periodStart) {
        public boolean overPool() {
            return pooledDistributedCent > availablePoolCent;
        }
    }

    // ---- 读投影 ----

    /**
     * S4 财务对账读投影:月度结算单头列表(倒序,最多 limit 单)。回答财务三个问题:
     * 这单收了多少综合服务费、池链支与专属支各多少、归了谁(收款主体中文名一并带出)。
     *
     * <p>带 pooled_distributed_cent(池分配合计,同 {@link #findOverPoolContext} 口径)——结算审阅页直接看到哪些单超池、超了多少
     * (第 75 条:超出部分过账时由娱乐平台公司承担,不设结算期审批)。
     * 第十八批第 2 条（ENUM L52「三者即使同归一个主体，也必须分三栏分别列示」）：未分配池余额、通票未核销减除额（及归属主体）、
     * 尾差三栏分列，另带暂挂待补配金额与第一段超池额。
     */
    public List<BillSummary> listBills(String projectId, java.time.LocalDate periodStart, int limit) {
        return jdbcTemplate.query("""
                SELECT b.id, b.project_id, b.period_start, b.period_end,
                       b.ticket_revenue_cent, b.channel_fee_cent, b.route_fee_cent,
                       b.comp_service_fee_pooled_cent, b.comp_service_fee_exclusive_cent,
                       b.fee_beneficiary_entity_id, be.entity_name AS fee_beneficiary_entity_name,
                       b.available_pool_cent, b.distributed_cent, b.combo_discount_cent,
                       b.rounding_residual_cent, b.distribution_method, b.status,
                       b.available_pool_cent + b.pool_overflow_cent AS pooled_distributed_cent,
                       b.pool_undistributed_cent, b.unverified_deduction_cent, b.unverified_deduction_entity_id,
                       ude.entity_name AS unverified_deduction_entity_name, b.pool_suspended_cent,
                       b.gift_cost_cent,
                       COALESCE(v.paid, 0)     AS gift_actual_paid_cent,
                       COALESCE(v.variance, 0) AS gift_variance_cent
                  FROM settlement_bills b
                  LEFT JOIN business_entities be ON be.id = b.fee_beneficiary_entity_id
                  LEFT JOIN business_entities ude ON ude.id = b.unverified_deduction_entity_id
                  -- S7 赠付差额(台账 append-only 净额:冲正行自带负号,SUM 即现值;差额一律留娱乐平台公司)
                  LEFT JOIN LATERAL (
                       SELECT SUM(l.actual_paid_cent)     AS paid,
                              SUM(l.variance_cent)        AS variance
                         FROM pass_gift_cost_variance_ledger l
                        WHERE l.bill_id = b.id) v ON TRUE
                 WHERE (:project_id::text IS NULL OR b.project_id = :project_id)
                   AND (:period_start::date IS NULL OR b.period_start = :period_start)
                 ORDER BY b.period_start DESC, b.id
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period_start", periodStart == null ? null : java.sql.Date.valueOf(periodStart))
                .addValue("limit", limit),
                (rs, i) -> new BillSummary(
                        rs.getString("id"), rs.getString("project_id"),
                        rs.getObject("period_start", java.time.LocalDate.class),
                        rs.getObject("period_end", java.time.LocalDate.class),
                        rs.getLong("ticket_revenue_cent"), rs.getLong("channel_fee_cent"), rs.getLong("route_fee_cent"),
                        rs.getLong("comp_service_fee_pooled_cent"), rs.getLong("comp_service_fee_exclusive_cent"),
                        rs.getString("fee_beneficiary_entity_id"), rs.getString("fee_beneficiary_entity_name"),
                        rs.getLong("available_pool_cent"), rs.getLong("distributed_cent"),
                        rs.getLong("combo_discount_cent"), rs.getLong("rounding_residual_cent"),
                        rs.getString("distribution_method"), rs.getString("status"),
                        rs.getLong("pooled_distributed_cent"),
                        rs.getLong("pool_undistributed_cent"), rs.getLong("unverified_deduction_cent"),
                        rs.getString("unverified_deduction_entity_id"), rs.getString("unverified_deduction_entity_name"),
                        rs.getLong("pool_suspended_cent"),
                        rs.getLong("gift_cost_cent"), rs.getLong("gift_actual_paid_cent"),
                        rs.getLong("gift_variance_cent")));
    }

    /**
     * S4 结算单头读投影(财务对账用,金额带符号:负数月=当月净追回)。
     * pooledDistributedCent(池分配合计,与 availablePoolCent 比对即知是否超池、超了多少;超池判定与 {@link OverPoolContext#overPool()} 同口径)。
     */
    public record BillSummary(String id, String projectId, java.time.LocalDate periodStart,
                              java.time.LocalDate periodEnd, long ticketRevenueCent, long channelFeeCent,
                              // 第十八批第 3 条：channelFeeCent＝销售渠道费（仅第三方渠道），routeFeeCent＝通路费（所有付款）。
                              long routeFeeCent,
                              long compServiceFeePooledCent, long compServiceFeeExclusiveCent,
                              String feeBeneficiaryEntityId, String feeBeneficiaryEntityName,
                              long availablePoolCent, long distributedCent, long comboDiscountCent,
                              long roundingResidualCent, String distributionMethod, String status,
                              long pooledDistributedCent,
                              // 第十八批第 2 条（ENUM L52「三者即使同归一个主体，也必须分三栏分别列示」）：未分配池余额、
                              //   通票未核销减除额（及归属主体）与尾差（roundingResidualCent）三栏分列；暂挂待补配。
                              long poolUndistributedCent, long unverifiedDeductionCent, String unverifiedDeductionEntityId,
                              String unverifiedDeductionEntityName, long poolSuspendedCent,
                              // S7 赠付差额三数:这个月预扣了多少(giftCost)、实际付出去多少(giftActualPaid)、
                              // 差额多少(giftVariance=预扣−实付,一律留娱乐平台公司;18B 复核后裁决第 11 条)。
                              long giftCostCent, long giftActualPaidCent, long giftVarianceCent) {
    }

    /**
     * 第十八批 AI 段 237-R2：审阅明细的业态份额行。interEntityNetCent＝域间份额（237-R1），unverifiedNetCent＝到期未核销归属（214-R2），
     * differenceCent＝净份额−按内部结算价直付额（ENUM:53 责任主体承担（负）或所得（正）的差额；无直付的行为空）。
     */
    public record FormatShareView(String businessFormat, String beneficiaryKind, String beneficiaryEntityId,
                                  String beneficiaryEntityName, long grossRevenueCent, long channelFeeCent, long routeFeeCent,
                                  long compServiceFeeCent, long giftCostCent, long giftActualPaidCent, long netRevenueCent,
                                  long interEntityNetCent, long unverifiedNetCent, long internalSettledCent, Long differenceCent) {
    }

    /** 第十八批 AI 段 237-R2：审阅明细的域内分配行（settlement_items 一行连商户名与业态）。 */
    public record ItemView(String merchantId, String merchantName, String businessFormat, String distributionMethod,
                           int verifiedCount, int guestCount, Integer rankNo, long finalAmountCent,
                           long singleBarRouteFeeCent, long singleBarChannelFeeCent, String status) {
    }

    /** 第十八批 AI 段 237-R2：审阅明细按实际提供方的内部结算价权益（赠饮应付净额、有价权益直付、零元平台出资直付）。 */
    public record ProviderInternalPrice(String merchantId, String merchantName, String businessFormat,
                                        long giftDrinkPaidCent, long paidDirectSettledCent, long freeDirectSettledCent) {
    }

    /**
     * 短期经营激励单次有效期（上限配置与申请期限同一读法）：数值 + 单位(day/month)。
     * 用户 2026-07-28 纠正「不是指校验大于等于一个月，也可以三天」——三天的短期激励必须配得出来。
     */
    public record SpecialApprovalTerm(int value, String unit) {

        public boolean byDay() {
            return "day".equals(unit);
        }

        /** from 起算 value 天/月后的那一天（月按月历：1月31日+3月=4月30日）。 */
        public java.time.LocalDate after(java.time.LocalDate from) {
            return byDay() ? from.plusDays(value) : from.plusMonths(value);
        }

        /** 给人看的写法：「3 天」/「3 个月」。总览页、草案摘要、审计留痕都用它，全站一个说法。 */
        public String label() {
            return value + (byDay() ? " 天" : " 个月");
        }
    }

    /**
     * V268 套票折扣归属配置读投影(§7.4)。splitPlatformBps 仅 split_by_ratio 用。
     * V369 roundingToleranceCent:综合/联合通票「凑整允许误差」(分)——运营填的近似售价与系统算出售价的差额上限,
     * 超出即拒绝发布。刻意与折扣归属同表同行:凑整差额走的就是这条折扣承担口径,运营只在一处维护。
     */
    public record ComboDiscountConfig(String discountBearer, int splitPlatformBps, long roundingToleranceCent) {
    }

    /** V268 综合服务费配置读投影(§7.2):fixedAmountCent 非空=用固定额,否则用 feeBps 比例。
     *  V336 monthlyCapAmountCent:该费率规则当月封顶(绝对分值);非空则封顶。id=解析到的规则主键,
     *  供引擎按「商户×规则」聚合封顶(同一商户多票种落同一条规则时封顶按合计压,honor「商户级规则=该商户当月封顶」)。 */
    /** S6 赠送权益预扣规则一行:predeductMode ∈ {by_item, by_ticket_amount};按票计必有整额、按件计恒 null(DB CHECK 锁死)。 */
    public record PassGiftCostConfig(String id, String predeductMode, Long ticketGiftAmountCent,
                                     Long monthlyCapAmountCent) {
    }

    public record ServiceFeeConfig(String id, int feeBps, Long fixedAmountCent, Long monthlyCapAmountCent) {
    }

    public record MerchantMonthlyStat(String merchantId, int verifiedCount, int guestCount) {
    }

    /** 批3-4 专属核销直付投影：某专属商户x票种当月全额收入 + 核销数/客数（仅供明细展示，不进池排名）。
     *  V268 加 entitlementType：综合服务费(§7.2)按 项目x商户x票种范围 查费率,故按票种拆聚合。
     *  D6 加 discountCent：该专属商户x票种当月套票折扣额合计（供引擎按归属把项目承担部分补回商户净额）。 */
    public record ExclusiveMerchantRevenue(String merchantId, String entitlementType, long revenueCent,
                                           long discountCent, int verifiedCount, int guestCount,
                                           long compServiceFeeCent) {
    }

    /** S11① 当月某业态的进池核销汇总：净额（客人实付拆到本业态的部分）+ 套票折扣额（供按承担方补回满额）
     *  + S4 本业态计提的门票综合服务费(池链支,从本业态净份额扣,归娱乐平台公司)。 */
    public record FormatRevenue(String businessFormat, long revenueCent, long discountCent, long compServiceFeeCent,
                                long giftCostCent, long routeFeeCent) {
    }

    /** 第十八批 X 段 176-R2：当月某业态有价专属直付的域份额、分摊的通路费与按内部结算价应直付额合计。 */
    public record ExclusiveDomainShare(String businessFormat, long domainShareCent, long routeFeeCent, long internalSettledCent) {
    }

    /** S4 当月进池核销两项合计(同一条查询同一套过滤):收入(池基)+ 门票综合服务费池链支(F_pool)。 */
    public record PooledTotals(long revenueCent, long compServiceFeeCent, long giftCostCent, long routeFeeCent) {
    }

    /**
     * V381 渠道抽成配置读投影:fixedAmountCent 非空=每张票固定金额,否则 feeBps 比例(照 ServiceFeeConfig 范式)。
     * FS-023:id 供到期未核销款逐笔记录所用渠道费配置(ENUM L51「每笔分摊的结果与所用合同版本必须留痕」)。
     */
    public record ChannelFeeConfig(String id, int feeBps, Long fixedAmountCent) {
    }

    /** 第十八批 AI 段 212-R1：单一酒吧门票某酒吧 × 销售渠道费配置的当月核销额、已分摊通路费与固定额锚点票数。 */
    public record SingleBarFee(String merchantId, ChannelFeeConfig fee, long revenueCent, long routeFeeCent, long ticketCount) {
    }

    /** 第十八批第 3 条：某支付通路的通路费率（万分比）与所用配置行 id（未配置＝0 / null）。 */
    public record RouteFee(String configId, int feeBps) {
    }

    /**
     * S11② 当月某销售渠道的进池核销实付额（渠道抽成整单前移的计费基数）+
     * V381 当期签名票数（固定金额模式计费基数,按首笔核销锚点计,冲正为负）。
     * 第十八批第 2 条 pooledAtSaleFeeCent：锚点票里有池快照的，快照已按酒吧份额扣过的那部分销售渠道费（固定金额模式扣回，整票只扣一次）。
     */
    public record ChannelRevenue(String salesChannel, ChannelFeeConfig fee, long revenueCent, long ticketCount,
                                 long pooledAtSaleFeeCent) {
    }
}
