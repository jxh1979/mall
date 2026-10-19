package com.complex.platform.settlement;

// 文件版本：V0.02

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
     * S11②/V381：某<b>销售渠道</b>的抽成配置——比例(万分比)或<b>每张票固定金额</b>(分)二选一
     * (chk_cfc_fee_mode 保证不双配)。优先级：项目特例 &gt; 全局默认；同级下 该渠道专属配置 &gt; 通用配置。
     * 渠道无专属配置 → 落回通用配置（默认种子 比例 0 = 官方直销不抽，逐分不变）。
     */
    public ChannelFeeConfig findChannelFee(String projectId, String channel) {
        ChannelFeeConfig cfg = jdbcTemplate.query("""
                SELECT id, fee_bps, fixed_amount_cent FROM channel_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE
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
     * S9 超池特批有效期：项目特例优先，否则全局默认（建表默认 3 个月 = 业务口径「期限可配，默认不超过 3 个月」）。
     * 与分配方式同表同维度——超池特批就是该分配方式的溢出出口，同一业务对象同一处配。
     *
     * <p>V368（用户 2026-07-28 纠正）：期限不再只能按月。三天的短期激励也要配得出来，
     * 所以读回「数值 + 单位」，单位 day/month。
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

    /** by_rank_tier 排名档位：项目特例整套优先，无项目档位则回退全局默认档位。 */
    public List<RankTier> findRankTiers(String projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("project_id", projectId);
        // 有项目特例档位则只取项目的；否则取全局默认（project_id IS NULL）。
        List<RankTier> project = jdbcTemplate.query("""
                SELECT rank_from, rank_to, share_bps FROM ticket_distribution_rank_tiers
                 WHERE status = 'active' AND is_draft = FALSE AND project_id = :project_id
                 ORDER BY rank_from
                """, params, (rs, i) -> new RankTier(rs.getInt("rank_from"), rs.getInt("rank_to"), rs.getInt("share_bps")));
        if (!project.isEmpty()) {
            return project;
        }
        return jdbcTemplate.query("""
                SELECT rank_from, rank_to, share_bps FROM ticket_distribution_rank_tiers
                 WHERE status = 'active' AND is_draft = FALSE AND project_id IS NULL
                 ORDER BY rank_from
                """, new MapSqlParameterSource(), (rs, i) ->
                new RankTier(rs.getInt("rank_from"), rs.getInt("rank_to"), rs.getInt("share_bps")));
    }

    /** V374 rule_type 值:第一段固定规则二选一(与 chk_tdmr_rule 一致)。 */
    public static final String RULE_PER_GUEST = "per_guest";
    public static final String RULE_FIXED = "fixed";

    /**
     * S8 两段分配·第一段名单:项目下全部生效的商户级固定规则(每商户一条,项目特例优先于全局默认)。
     * <b>有行 = 该酒吧属第一段(FixedSet)</b>,按 rule_type 二选一取绝对额;无行 = 属第二段(排名档位)。
     * 一次取全名单(引擎按月跑一次,商户数十级,DISTINCT ON 每商户取最优先一行)。
     */
    public java.util.Map<String, MerchantRateRule> findActiveMerchantRateRules(String projectId) {
        java.util.Map<String, MerchantRateRule> out = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT DISTINCT ON (merchant_id) merchant_id, rule_type, per_guest_cent, fixed_amount_cent
                  FROM ticket_distribution_merchant_rates
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY merchant_id, (project_id IS NOT NULL) DESC
                """, new MapSqlParameterSource().addValue("project_id", projectId), rs -> {
            out.put(rs.getString("merchant_id"), new MerchantRateRule(
                    rs.getString("rule_type"),
                    rs.getObject("per_guest_cent") == null ? null : rs.getLong("per_guest_cent"),
                    rs.getObject("fixed_amount_cent") == null ? null : rs.getLong("fixed_amount_cent")));
        });
        return out;
    }

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
                SELECT COALESCE(SUM(verified_amount_cent), 0)  AS revenue_cent,
                       COALESCE(SUM(comp_service_fee_cent), 0) AS comp_service_fee_cent,
                       COALESCE(SUM(gift_cost_cent), 0)        AS gift_cost_cent
                  FROM settlement_transactions st
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND verification_id IS NOT NULL
                   AND exclusive_merchant_id IS NULL
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff),
                (rs, i) -> new PooledTotals(rs.getLong("revenue_cent"), rs.getLong("comp_service_fee_cent"),
                        rs.getLong("gift_cost_cent")));
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
        // S4:各业态行带上本业态计提的综合服务费——费跟着核销走,游乐/餐饮份额的费从各自净份额扣,
        //   不错扣进酒吧池;Σ各业态费 与 sumPooledTotals 的费合计由引擎 K20 运行期互核。
        // S6:赠送权益预估成本(G)同一范式跟核销走——计提落在哪行核销上就扣在那行所属业态的份额里,
        //   别的业态不替它埋单;常态首笔核销=入场(酒吧业态)→G 落酒吧行,与 HTML:742 瀑布顺序一致。
        return jdbcTemplate.query("""
                SELECT COALESCE(m.merchant_type, 'bar')            AS business_format,
                       COALESCE(SUM(st.verified_amount_cent), 0)   AS revenue_cent,
                       COALESCE(SUM(st.discount_amount_cent), 0)   AS discount_cent,
                       COALESCE(SUM(st.comp_service_fee_cent), 0)  AS comp_service_fee_cent,
                       COALESCE(SUM(st.gift_cost_cent), 0)         AS gift_cost_cent
                  FROM settlement_transactions st
                  LEFT JOIN merchants m ON m.id = st.merchant_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NULL
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new FormatRevenue(rs.getString("business_format"),
                        rs.getLong("revenue_cent"), rs.getLong("discount_cent"),
                        rs.getLong("comp_service_fee_cent"), rs.getLong("gift_cost_cent")));
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
                       COALESCE(SUM(st.verified_amount_cent), 0)  AS revenue_cent,
                       COALESCE(SUM(CASE WHEN a.verification_id IS NOT NULL
                                         THEN CASE st.transaction_type WHEN 'verification_settled' THEN 1 ELSE -1 END
                                         ELSE 0 END), 0)          AS ticket_count
                  FROM settlement_transactions st
                  LEFT JOIN ticket_orders o ON o.id = st.order_id
                  LEFT JOIN (SELECT DISTINCT ON (parent_ticket_id) parent_ticket_id, verification_id
                               FROM settlement_transactions
                              WHERE transaction_type = 'verification_settled'
                                AND verification_id IS NOT NULL
                                AND exclusive_merchant_id IS NULL
                                AND parent_ticket_id IS NOT NULL
                                AND (:project_id::text IS NULL OR project_id = :project_id)
                              ORDER BY parent_ticket_id, COALESCE(business_at, created_at), id) a
                         ON a.parent_ticket_id = st.parent_ticket_id AND a.verification_id = st.verification_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NULL
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new ChannelRevenue(rs.getString("sales_channel"), rs.getLong("revenue_cent"),
                        rs.getLong("ticket_count")));
    }

    /**
     * 当月各酒吧统计：按 merchant_id 聚合 核销数(Σverified_count) + 客数(Σnum_people)。仅归属商户的核销。
     * S11①：<b>只算酒吧业态</b>——游乐/餐饮/零售的份额不进酒吧池，自然也不该参与酒吧的排名与客数聚合，
     * 否则一家餐厅会因为核销了几张餐券而占掉某个酒吧的分池名次。业态条件与 {@link #aggregateFormatRevenue} 同源。
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
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY st.merchant_id
                 ORDER BY verified_count DESC, st.merchant_id
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

    /** FS-021-R2：本窗内被撤销核销所属的、早于本窗的原月（UTC 月首），按月升序。 */
    public List<java.time.LocalDate> findRecoveredOriginalMonths(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT (date_trunc('month', COALESCE(o.business_at, o.created_at) AT TIME ZONE 'UTC'))::date
                  FROM settlement_transactions st
                  JOIN settlement_transactions o ON o.transaction_no = 'ST-VS-' || st.verification_id
                 WHERE st.transaction_type = 'verification_reversed'
                   AND COALESCE(st.business_at, st.created_at) >= :start AND COALESCE(st.business_at, st.created_at) < :end
                   AND COALESCE(o.business_at, o.created_at) < :start
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart), java.time.LocalDate.class);
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
     */
    public List<GiftPaid> aggregateGiftPaid(String projectId, Instant monthStart, Instant nextMonthStart,
                                            Instant recoveryCutoff) {
        return jdbcTemplate.query("""
                SELECT st.ticket_type_id,
                       COALESCE(m.merchant_type, 'bar')       AS business_format,
                       COALESCE(SUM(CASE WHEN a.direction = 'payable' THEN a.amount_cent
                                         ELSE -a.amount_cent END), 0) AS paid_cent
                  FROM ar_ap_items a
                  JOIN settlement_transactions st
                    ON st.verification_id = a.source_id
                   AND st.transaction_type = CASE a.direction WHEN 'payable' THEN 'verification_settled'
                                                              ELSE 'verification_reversed' END
                  LEFT JOIN merchants m ON m.id = a.counterparty_id
                 WHERE a.source_type = 'pass_drink_settlement'
                """ + WINDOW + """
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1, 2
                 ORDER BY 1, 2
                """, monthWindow(projectId, monthStart, nextMonthStart, recoveryCutoff), (rs, i) ->
                new GiftPaid(rs.getString("ticket_type_id"), rs.getString("business_format"),
                        rs.getLong("paid_cent")));
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

    /**
     * S7 carry_forward 跨期滚动:该「项目 × 规则」在本期之前【结转下期且尚未被处理】的净额
     * = Σ往期 carried_forward − Σ往期 carried_in(往期行含冲正行,取反后天然净额正确)。
     * 不逐期链式取上一期——中间某期无单/漏结时,滚存额在下一次结算被一并拾起,不丢不重。
     */
    public long sumPriorCarry(String projectId, String configId, java.time.LocalDate periodStart) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(carried_forward_cent - carried_in_cent), 0)
                  FROM pass_gift_cost_variance_ledger
                 WHERE config_id = :config_id
                   AND project_id IS NOT DISTINCT FROM :project_id
                   AND period_start < :period_start
                """, new MapSqlParameterSource()
                .addValue("config_id", configId)
                .addValue("project_id", projectId)
                .addValue("period_start", java.sql.Date.valueOf(periodStart)), Long.class);
        return sum == null ? 0L : sum;
    }

    /** S7 本单台账现状:按规则聚合净额(append-only 全行相加,冲正行自带负号) + 最新非冲正行 id(供冲正指认)。 */
    public java.util.Map<String, VarianceNet> findVarianceNetByBill(String billId) {
        java.util.Map<String, VarianceNet> out = new java.util.LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT config_id,
                       COALESCE(SUM(accrued_cost_cent), 0)    AS accrued,
                       COALESCE(SUM(actual_paid_cent), 0)     AS paid,
                       COALESCE(SUM(carried_in_cent), 0)      AS carried_in,
                       COALESCE(SUM(carried_forward_cent), 0) AS carried_forward,
                       COALESCE(SUM(pool_true_up_cent), 0)    AS pool_true_up,
                       MAX(id) FILTER (WHERE reversal_of IS NULL) AS latest_statement_id
                  FROM pass_gift_cost_variance_ledger
                 WHERE bill_id = :bill_id
                 GROUP BY config_id
                """, new MapSqlParameterSource().addValue("bill_id", billId), rs -> {
            out.put(rs.getString("config_id"), new VarianceNet(
                    rs.getLong("accrued"), rs.getLong("paid"), rs.getLong("carried_in"),
                    rs.getLong("carried_forward"), rs.getLong("pool_true_up"),
                    rs.getString("latest_statement_id")));
        });
        return out;
    }

    /** S7 追加一行差额台账(append-only,只 INSERT;更正由调用方追加冲正行,绝不 UPDATE)。 */
    public void insertVarianceRow(VarianceLedgerRow row) {
        jdbcTemplate.update("""
                INSERT INTO pass_gift_cost_variance_ledger (
                    id, bill_id, project_id, period_start, period_end, config_id, variance_handling,
                    accrued_cost_cent, actual_paid_cent, variance_cent,
                    carried_in_cent, carried_forward_cent, pool_true_up_cent,
                    beneficiary_entity_id, reversal_of)
                VALUES (:id, :bill_id, :project_id, :period_start, :period_end, :config_id, :variance_handling,
                        :accrued, :paid, :variance, :carried_in, :carried_forward, :pool_true_up,
                        :beneficiary_entity_id, :reversal_of)
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
                .addValue("carried_in", row.carriedInCent())
                .addValue("carried_forward", row.carriedForwardCent())
                .addValue("pool_true_up", row.poolTrueUpCent())
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
    public record VarianceNet(long accruedCostCent, long actualPaidCent, long carriedInCent,
                              long carriedForwardCent, long poolTrueUpCent, String latestStatementId) {

        public long varianceCent() {
            return accruedCostCent - actualPaidCent;
        }

        boolean isZero() {
            return accruedCostCent == 0L && actualPaidCent == 0L && carriedInCent == 0L
                    && carriedForwardCent == 0L && poolTrueUpCent == 0L;
        }
    }

    /** S7 差额台账一行(冲正行=全金额取反 + reversalOf 指向被冲行)。 */
    public record VarianceLedgerRow(String id, String billId, String projectId,
                                    java.time.LocalDate periodStart, java.time.LocalDate periodEnd,
                                    String configId, String varianceHandling,
                                    long accruedCostCent, long actualPaidCent, long varianceCent,
                                    long carriedInCent, long carriedForwardCent, long poolTrueUpCent,
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
                .addValue("available_pool_cent", bill.availablePoolCent())
                .addValue("distributed_cent", bill.distributedCent())
                .addValue("comp_service_fee_pooled_cent", bill.compServiceFeePooledCent())
                .addValue("comp_service_fee_exclusive_cent", bill.compServiceFeeExclusiveCent())
                .addValue("fee_beneficiary_entity_id", bill.feeBeneficiaryEntityId())
                .addValue("gift_cost_cent", bill.giftCostCent())
                .addValue("gift_beneficiary_entity_id", bill.giftBeneficiaryEntityId())
                .addValue("distribution_method", bill.distributionMethod())
                .addValue("special_approval_ref", bill.specialApprovalRef())
                .addValue("status", bill.status())
                .addValue("attributed_entity_id", bill.attributedEntityId())
                .addValue("combo_discount_cent", bill.comboDiscountCent())
                .addValue("rounding_residual_cent", bill.roundingResidualCent())
                .addValue("rounding_residual_entity_id", bill.roundingResidualEntityId())
                .addValue("pool_undistributed_cent", bill.poolUndistributedCent())
                .addValue("pool_undistributed_entity_id", bill.poolUndistributedEntityId());
        jdbcTemplate.update("""
                INSERT INTO settlement_bills (
                    id, project_id, period_start, period_end, ticket_revenue_cent, channel_fee_cent,
                    available_pool_cent, distributed_cent,
                    comp_service_fee_pooled_cent, comp_service_fee_exclusive_cent, fee_beneficiary_entity_id,
                    gift_cost_cent, gift_beneficiary_entity_id,
                    distribution_method, special_approval_ref, status,
                    attributed_entity_id, combo_discount_cent, rounding_residual_cent,
                    rounding_residual_entity_id,
                    pool_undistributed_cent, pool_undistributed_entity_id, updated_at
                )
                VALUES (
                    :id, :project_id, :period_start, :period_end, :ticket_revenue_cent, :channel_fee_cent,
                    :available_pool_cent, :distributed_cent,
                    :comp_service_fee_pooled_cent, :comp_service_fee_exclusive_cent, :fee_beneficiary_entity_id,
                    :gift_cost_cent, :gift_beneficiary_entity_id,
                    :distribution_method, :special_approval_ref, :status,
                    :attributed_entity_id, :combo_discount_cent, :rounding_residual_cent,
                    :rounding_residual_entity_id,
                    :pool_undistributed_cent, :pool_undistributed_entity_id, now()
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), period_start, period_end) DO UPDATE SET
                    ticket_revenue_cent = EXCLUDED.ticket_revenue_cent,
                    channel_fee_cent    = EXCLUDED.channel_fee_cent,
                    available_pool_cent = EXCLUDED.available_pool_cent,
                    distributed_cent    = EXCLUDED.distributed_cent,
                    comp_service_fee_pooled_cent    = EXCLUDED.comp_service_fee_pooled_cent,
                    comp_service_fee_exclusive_cent = EXCLUDED.comp_service_fee_exclusive_cent,
                    fee_beneficiary_entity_id       = EXCLUDED.fee_beneficiary_entity_id,
                    gift_cost_cent                  = EXCLUDED.gift_cost_cent,
                    gift_beneficiary_entity_id      = EXCLUDED.gift_beneficiary_entity_id,
                    distribution_method = EXCLUDED.distribution_method,
                    -- D7 防覆盖：已填的特批引用不被 settleMonth 重跑清 null（引擎恒写 null）；status 一旦特批过
                    --   （special_approval_ref 非空）就锁死不被重跑打回 draft，其余（未特批）单照常按引擎新算态覆盖。
                    -- S9 到期回收：冻结只在【特批仍在有效期内】时成立；到期后本单回到普通单待遇——按引擎新算态
                    --   覆盖 status（仍超池即打回 draft，须平台负责人重新特批才能再过账）。过期引用不清空（留痕保全）。
                    special_approval_ref = COALESCE(settlement_bills.special_approval_ref, EXCLUDED.special_approval_ref),
                    status              = CASE WHEN settlement_bills.special_approval_ref IS NOT NULL
                                                AND settlement_bills.special_approval_valid_until >= CURRENT_DATE
                                               THEN settlement_bills.status ELSE EXCLUDED.status END,
                    attributed_entity_id = EXCLUDED.attributed_entity_id,
                    combo_discount_cent = EXCLUDED.combo_discount_cent,
                    rounding_residual_cent = EXCLUDED.rounding_residual_cent,
                    rounding_residual_entity_id = EXCLUDED.rounding_residual_entity_id,
                    pool_undistributed_cent      = EXCLUDED.pool_undistributed_cent,
                    pool_undistributed_entity_id = EXCLUDED.pool_undistributed_entity_id,
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
     *  S7 补本业态兑付实付列 gift_actual_paid_cent——纯留痕不进净份额）。 */
    public void upsertFormatShare(SettlementBillFormatShareRecord share) {
        jdbcTemplate.update("""
                INSERT INTO settlement_bill_format_shares (
                    bill_id, business_format, gross_revenue_cent, platform_borne_discount_cent,
                    channel_fee_cent, comp_service_fee_cent, gift_cost_cent, gift_actual_paid_cent,
                    net_revenue_cent, beneficiary_kind, beneficiary_entity_id)
                VALUES (:bill_id, :business_format, :gross, :discount, :channel_fee, :comp_service_fee,
                        :gift_cost, :gift_actual_paid, :net, :beneficiary_kind, :beneficiary_entity_id)
                ON CONFLICT (bill_id, business_format) DO UPDATE SET
                    gross_revenue_cent           = EXCLUDED.gross_revenue_cent,
                    platform_borne_discount_cent = EXCLUDED.platform_borne_discount_cent,
                    channel_fee_cent             = EXCLUDED.channel_fee_cent,
                    comp_service_fee_cent        = EXCLUDED.comp_service_fee_cent,
                    gift_cost_cent               = EXCLUDED.gift_cost_cent,
                    gift_actual_paid_cent        = EXCLUDED.gift_actual_paid_cent,
                    net_revenue_cent             = EXCLUDED.net_revenue_cent,
                    beneficiary_kind             = EXCLUDED.beneficiary_kind,
                    beneficiary_entity_id        = EXCLUDED.beneficiary_entity_id,
                    updated_at                   = now()
                """, new MapSqlParameterSource()
                .addValue("bill_id", share.billId())
                .addValue("business_format", share.businessFormat())
                .addValue("gross", share.grossRevenueCent())
                .addValue("discount", share.platformBorneDiscountCent())
                .addValue("channel_fee", share.channelFeeCent())
                .addValue("comp_service_fee", share.compServiceFeeCent())
                .addValue("gift_cost", share.giftCostCent())
                .addValue("gift_actual_paid", share.giftActualPaidCent())
                .addValue("net", share.netRevenueCent())
                .addValue("beneficiary_kind", share.beneficiaryKind())
                .addValue("beneficiary_entity_id", share.beneficiaryEntityId()));
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
                .addValue("status", item.status());
        jdbcTemplate.update("""
                INSERT INTO settlement_items (
                    id, bill_id, merchant_id, verified_count, guest_count, rank_no, distribution_method,
                    calculated_amount_cent, final_amount_cent, status, updated_at
                )
                VALUES (
                    :id, :bill_id, :merchant_id, :verified_count, :guest_count, :rank_no, :distribution_method,
                    :calculated_amount_cent, :final_amount_cent, :status, now()
                )
                ON CONFLICT (bill_id, merchant_id, distribution_method) DO UPDATE SET
                    verified_count         = EXCLUDED.verified_count,
                    guest_count            = EXCLUDED.guest_count,
                    rank_no                = EXCLUDED.rank_no,
                    distribution_method    = EXCLUDED.distribution_method,
                    calculated_amount_cent = EXCLUDED.calculated_amount_cent,
                    final_amount_cent      = EXCLUDED.final_amount_cent,
                    status                 = EXCLUDED.status,
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

    /**
     * D7(对抗复核 risk2):该月份结算单【仍在有效期内】的 special_approval_ref(未特批/已过期/无单返 null);
     * 供 settleMonth 冻结已特批单防重算漂移。
     *
     * <p>S9 到期回收:过期特批不再冻结——引擎照常重算,仍超池就打回 draft 待重新特批。
     */
    public String findActiveSpecialApprovalRefByPeriod(String projectId, java.time.LocalDate periodStart,
            java.time.LocalDate periodEnd) {
        return jdbcTemplate.query("""
                SELECT special_approval_ref FROM settlement_bills
                 WHERE COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                   AND period_start = :period_start AND period_end = :period_end
                   AND special_approval_valid_until >= CURRENT_DATE
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period_start", java.sql.Date.valueOf(periodStart))
                .addValue("period_end", java.sql.Date.valueOf(periodEnd)),
                rs -> rs.next() ? rs.getString("special_approval_ref") : null);
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

    // ---- D7 超池特批出口 + 超额 C 口径 ----

    /**
     * D7 过账/特批用超池上下文：结算单状态 + 可分配池 availablePool + <b>池分账</b>合计 pooledDistributed
     * （仅 distribution_method != exclusive_full 的明细，专属直付不占池、不参与越池判定）。
     * overPool = pooledDistributed > availablePool；单不存在返回 null。
     * S4:池就一个——available_pool_cent(酒吧净份额 − 池链综合服务费),原 spv_pool_cent 已随留存链删除。
     */
    public OverPoolContext findOverPoolContext(String billId) {
        return jdbcTemplate.query("""
                SELECT b.status, b.project_id, b.period_start, b.available_pool_cent,
                       b.special_approval_valid_until,
                       (b.special_approval_ref IS NOT NULL
                        AND b.special_approval_valid_until >= CURRENT_DATE) AS special_approval_active,
                       COALESCE((SELECT SUM(i.final_amount_cent) FROM settlement_items i
                                  WHERE i.bill_id = b.id
                                    AND i.distribution_method IN (""" + POOL_METHODS_SQL + """
                )), 0) AS pooled_distributed_cent
                  FROM settlement_bills b
                 WHERE b.id = :id
                """, new MapSqlParameterSource().addValue("id", billId),
                rs -> rs.next()
                        ? new OverPoolContext(rs.getString("status"), rs.getString("project_id"),
                                rs.getLong("available_pool_cent"), rs.getLong("pooled_distributed_cent"),
                                rs.getBoolean("special_approval_active"),
                                rs.getObject("special_approval_valid_until", java.time.LocalDate.class),
                                rs.getObject("period_start", java.time.LocalDate.class))
                        : null);
    }

    /**
     * D7 特批出口：填 special_approval_ref 并把 status draft→confirmed（仅当仍 draft，防重复特批/并发覆盖）。
     * S9：同时按配置期限落到期日 = 特批当日 + termMonths 月（DB 单一时钟，与到期判定 CURRENT_DATE 同源）。
     *
     * @return 落库的到期日；null = 未更新任何行（非 draft，调用方据此判 409）
     */
    public java.time.LocalDate markBillSpecialApproved(String billId, String specialApprovalRef,
                                                       SpecialApprovalTerm term) {
        return jdbcTemplate.query("""
                UPDATE settlement_bills
                   SET special_approval_ref = :ref,
                       special_approval_valid_until =
                           (CURRENT_DATE + make_interval(months => :months, days => :days))::date,
                       status = 'confirmed', updated_at = now()
                 WHERE id = :id AND status = 'draft'
                RETURNING special_approval_valid_until
                """, new MapSqlParameterSource()
                .addValue("id", billId).addValue("ref", specialApprovalRef)
                // V368：一次 make_interval 同时收两种单位——配「月」走月历(1月31日+3月=4月30日)、
                //   配「天」走自然日，另一格为 0。到期日仍在 DB 侧算，与到期判定同一时钟。
                .addValue("months", term.months()).addValue("days", term.days()),
                rs -> rs.next() ? rs.getObject("special_approval_valid_until", java.time.LocalDate.class) : null);
    }

    /**
     * D7 超池上下文读投影：overPool = pooledDistributedCent &gt; availablePoolCent。
     * S9：specialApprovalActive = 有特批引用且到期日 ≥ 今日（DB 侧算，单一时钟）。
     */
    public record OverPoolContext(String status, String projectId, long availablePoolCent, long pooledDistributedCent,
                                  boolean specialApprovalActive, java.time.LocalDate specialApprovalValidUntil,
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
     * <p>超池特批入口(S9 遗留补齐):加带 special_approval_ref / special_approval_valid_until 与
     * pooled_distributed_cent(池分配合计,同 {@link #findOverPoolContext} 口径)——平台负责人在结算审阅页
     * 直接看到哪些单因超池停在 draft、超了多少、特批后有效到哪天。
     */
    public List<BillSummary> listBills(String projectId, java.time.LocalDate periodStart, int limit) {
        return jdbcTemplate.query("""
                SELECT b.id, b.project_id, b.period_start, b.period_end,
                       b.ticket_revenue_cent, b.channel_fee_cent,
                       b.comp_service_fee_pooled_cent, b.comp_service_fee_exclusive_cent,
                       b.fee_beneficiary_entity_id, be.entity_name AS fee_beneficiary_entity_name,
                       b.available_pool_cent, b.distributed_cent, b.combo_discount_cent,
                       b.rounding_residual_cent, b.distribution_method, b.status,
                       b.special_approval_ref, b.special_approval_valid_until,
                       COALESCE((SELECT SUM(i.final_amount_cent) FROM settlement_items i
                                  WHERE i.bill_id = b.id
                                    AND i.distribution_method IN (""" + POOL_METHODS_SQL + """
                )), 0) AS pooled_distributed_cent,
                       b.gift_cost_cent,
                       COALESCE(v.paid, 0)     AS gift_actual_paid_cent,
                       COALESCE(v.variance, 0) AS gift_variance_cent,
                       COALESCE(v.carried, 0)  AS gift_carried_forward_cent,
                       COALESCE(v.true_up, 0)  AS gift_pool_true_up_cent,
                       v.handlings             AS gift_variance_handling
                  FROM settlement_bills b
                  LEFT JOIN business_entities be ON be.id = b.fee_beneficiary_entity_id
                  -- S7 赠付差额(台账 append-only 净额:冲正行自带负号,SUM 即现值;handlings 通常单值)
                  LEFT JOIN LATERAL (
                       SELECT SUM(l.actual_paid_cent)     AS paid,
                              SUM(l.variance_cent)        AS variance,
                              SUM(l.carried_forward_cent) AS carried,
                              SUM(l.pool_true_up_cent)    AS true_up,
                              string_agg(DISTINCT l.variance_handling, ',') AS handlings
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
                        rs.getLong("ticket_revenue_cent"), rs.getLong("channel_fee_cent"),
                        rs.getLong("comp_service_fee_pooled_cent"), rs.getLong("comp_service_fee_exclusive_cent"),
                        rs.getString("fee_beneficiary_entity_id"), rs.getString("fee_beneficiary_entity_name"),
                        rs.getLong("available_pool_cent"), rs.getLong("distributed_cent"),
                        rs.getLong("combo_discount_cent"), rs.getLong("rounding_residual_cent"),
                        rs.getString("distribution_method"), rs.getString("status"),
                        rs.getString("special_approval_ref"),
                        rs.getObject("special_approval_valid_until", java.time.LocalDate.class),
                        rs.getLong("pooled_distributed_cent"),
                        rs.getLong("gift_cost_cent"), rs.getLong("gift_actual_paid_cent"),
                        rs.getLong("gift_variance_cent"), rs.getLong("gift_carried_forward_cent"),
                        rs.getLong("gift_pool_true_up_cent"), rs.getString("gift_variance_handling")));
    }

    /**
     * S4 结算单头读投影(财务对账用,金额带符号:负数月=当月净追回)。
     * 特批三列:specialApprovalRef/specialApprovalValidUntil(已特批单)、pooledDistributedCent(池分配合计,
     * 与 availablePoolCent 比对即知是否超池、超了多少;超池判定与 {@link OverPoolContext#overPool()} 同口径)。
     */
    public record BillSummary(String id, String projectId, java.time.LocalDate periodStart,
                              java.time.LocalDate periodEnd, long ticketRevenueCent, long channelFeeCent,
                              long compServiceFeePooledCent, long compServiceFeeExclusiveCent,
                              String feeBeneficiaryEntityId, String feeBeneficiaryEntityName,
                              long availablePoolCent, long distributedCent, long comboDiscountCent,
                              long roundingResidualCent, String distributionMethod, String status,
                              String specialApprovalRef, java.time.LocalDate specialApprovalValidUntil,
                              long pooledDistributedCent,
                              // S7 赠付差额四数 + 处理规则(giftVarianceHandling 通常单值;多规则以逗号并列):
                              // 财务四问——这个月预扣了多少(giftCost)、实际付出去多少(giftActualPaid)、
                              // 差额多少(giftVariance=预扣−实付)、走的哪条规则。
                              long giftCostCent, long giftActualPaidCent, long giftVarianceCent,
                              long giftCarriedForwardCent, long giftPoolTrueUpCent,
                              String giftVarianceHandling) {
    }

    /**
     * S9/V368 超池特批有效期读投影：数值 + 单位(day/month)。
     * 用户 2026-07-28 纠正「不是指校验大于等于一个月，也可以三天」——三天的短期激励必须配得出来。
     */
    public record SpecialApprovalTerm(int value, String unit) {

        public boolean byDay() {
            return "day".equals(unit);
        }

        int months() {
            return byDay() ? 0 : value;
        }

        int days() {
            return byDay() ? value : 0;
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

    public record RankTier(int rankFrom, int rankTo, int shareBps) {
    }

    /** S8 第一段固定规则(V374 二选一):per_guest 时 perGuestCent 非空,fixed 时 fixedAmountCent 非空(DB CHECK 保证)。 */
    public record MerchantRateRule(String ruleType, Long perGuestCent, Long fixedAmountCent) {
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
                                long giftCostCent) {
    }

    /** S4 当月进池核销两项合计(同一条查询同一套过滤):收入(池基)+ 门票综合服务费池链支(F_pool)。 */
    public record PooledTotals(long revenueCent, long compServiceFeeCent, long giftCostCent) {
    }

    /**
     * V381 渠道抽成配置读投影:fixedAmountCent 非空=每张票固定金额,否则 feeBps 比例(照 ServiceFeeConfig 范式)。
     * FS-023:id 供到期未核销款逐笔记录所用渠道费配置(ENUM L51「每笔分摊的结果与所用合同版本必须留痕」)。
     */
    public record ChannelFeeConfig(String id, int feeBps, Long fixedAmountCent) {
    }

    /**
     * S11② 当月某销售渠道的进池核销实付额（渠道抽成整单前移的计费基数）+
     * V381 当期签名票数（固定金额模式计费基数,按首笔核销锚点计,冲正为负）。
     */
    public record ChannelRevenue(String salesChannel, long revenueCent, long ticketCount) {
    }
}
