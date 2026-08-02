package com.complex.platform.settlement;

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

    public JdbcSettlementBillRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---- 读配置（COALESCE 默认+项目特例；无配置回退建表默认值）----
    // S4:原「留存万分比」finder 已删——池内留存与门票综合服务费为同一项、只扣一次(业务 HTML),
    //   费率统一走 findComprehensiveServiceFee(platform_retention_rules 已随 V370 停用)。

    /** 第三方渠道抽成万分比（通用率，channel 未知/无订单时用）。 */
    public int findChannelFeeBps(String projectId) {
        return findChannelFeeBps(projectId, null);
    }

    /**
     * S11②：某<b>销售渠道</b>的抽成万分比。优先级：项目特例 &gt; 全局默认；同级下 该渠道专属率 &gt; 通用率。
     * 渠道无专属配置 → 落回通用率（默认种子 0 = 官方直销不抽，逐分不变）。
     */
    public int findChannelFeeBps(String projectId, String channel) {
        Integer bps = jdbcTemplate.query("""
                SELECT fee_bps FROM channel_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND (channel IS NULL OR channel = :channel)
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC, (channel IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                        .addValue("project_id", projectId)
                        .addValue("channel", channel),
                rs -> rs.next() ? rs.getInt("fee_bps") : null);
        return requireConfigured(bps, "channel_fee_configs.fee_bps", projectId);
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
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(verified_amount_cent), 0)  AS revenue_cent,
                       COALESCE(SUM(comp_service_fee_cent), 0) AS comp_service_fee_cent,
                       COALESCE(SUM(gift_cost_cent), 0)        AS gift_cost_cent
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND verification_id IS NOT NULL
                   AND exclusive_merchant_id IS NULL
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                """, monthWindow(projectId, monthStart, nextMonthStart),
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
    public List<FormatRevenue> aggregateFormatRevenue(String projectId, Instant monthStart, Instant nextMonthStart) {
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
                   AND COALESCE(st.business_at, st.created_at) >= :start
                   AND COALESCE(st.business_at, st.created_at) < :end
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) ->
                new FormatRevenue(rs.getString("business_format"),
                        rs.getLong("revenue_cent"), rs.getLong("discount_cent"),
                        rs.getLong("comp_service_fee_cent"), rs.getLong("gift_cost_cent")));
    }

    /**
     * S11②：当月<b>进池</b>核销按【销售渠道】汇总实付额（渠道抽成整单前移的计费基数）。
     * 渠道取自出票订单 {@code ticket_orders.sales_channel}；direct 出票无订单 → 空串（落通用率）。
     */
    public List<ChannelRevenue> aggregateChannelRevenue(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.query("""
                SELECT COALESCE(o.sales_channel, '')              AS sales_channel,
                       COALESCE(SUM(st.verified_amount_cent), 0)  AS revenue_cent
                  FROM settlement_transactions st
                  LEFT JOIN ticket_orders o ON o.id = st.order_id
                 WHERE st.transaction_type IN ('verification_settled','verification_reversed')
                   AND st.verification_id IS NOT NULL
                   AND st.exclusive_merchant_id IS NULL
                   AND COALESCE(st.business_at, st.created_at) >= :start
                   AND COALESCE(st.business_at, st.created_at) < :end
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY 1
                 ORDER BY 1
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) ->
                new ChannelRevenue(rs.getString("sales_channel"), rs.getLong("revenue_cent")));
    }

    /**
     * 当月各酒吧统计：按 merchant_id 聚合 核销数(Σverified_count) + 客数(Σnum_people)。仅归属商户的核销。
     * S11①：<b>只算酒吧业态</b>——游乐/餐饮/零售的份额不进酒吧池，自然也不该参与酒吧的排名与客数聚合，
     * 否则一家餐厅会因为核销了几张餐券而占掉某个酒吧的分池名次。业态条件与 {@link #aggregateFormatRevenue} 同源。
     */
    public List<MerchantMonthlyStat> aggregateMerchantStats(String projectId, Instant monthStart, Instant nextMonthStart) {
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
                   AND COALESCE(st.business_at, st.created_at) >= :start
                   AND COALESCE(st.business_at, st.created_at) < :end
                   AND (:project_id::text IS NULL OR st.project_id = :project_id)
                 GROUP BY st.merchant_id
                 ORDER BY verified_count DESC, st.merchant_id
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) ->
                new MerchantMonthlyStat(rs.getString("merchant_id"),
                        rs.getInt("verified_count"), rs.getInt("guest_count")));
    }

    /**
     * 结算重构批3-4(§6.4)：当月<b>专属</b>核销按 exclusive_merchant_id 分组 Σ verified_amount_cent。
     * 每个专属商户一条 → 全额直付明细（method=exclusive_full，不进可分配池、不稀释他家分池）。
     * 按落定在 settlement_transactions 的 exclusive_merchant_id 过滤（非 join 活模板，历史结算不受改模板影响）。
     */
    public List<ExclusiveMerchantRevenue> aggregateExclusiveRevenue(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.query("""
                SELECT exclusive_merchant_id AS merchant_id,
                       entitlement_type,
                       COALESCE(SUM(verified_amount_cent), 0) AS revenue_cent,
                       COALESCE(SUM(discount_amount_cent), 0) AS discount_cent,
                       COALESCE(SUM(verified_count), 0)       AS verified_count,
                       COALESCE(SUM(num_people), 0)           AS guest_count,
                       COALESCE(SUM(comp_service_fee_cent), 0) AS comp_service_fee_cent
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND verification_id IS NOT NULL
                   AND exclusive_merchant_id IS NOT NULL
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                 GROUP BY exclusive_merchant_id, entitlement_type
                 ORDER BY exclusive_merchant_id, entitlement_type
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) ->
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
        return new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("start", Timestamp.from(monthStart))
                .addValue("end", Timestamp.from(nextMonthStart));
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
                    pool_undistributed_cent, pool_undistributed_entity_id, updated_at
                )
                VALUES (
                    :id, :project_id, :period_start, :period_end, :ticket_revenue_cent, :channel_fee_cent,
                    :available_pool_cent, :distributed_cent,
                    :comp_service_fee_pooled_cent, :comp_service_fee_exclusive_cent, :fee_beneficiary_entity_id,
                    :gift_cost_cent, :gift_beneficiary_entity_id,
                    :distribution_method, :special_approval_ref, :status,
                    :attributed_entity_id, :combo_discount_cent, :rounding_residual_cent,
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

    /** S11：写一行结算单 × 业态份额留痕（V366;S4 补本业态综合服务费列;S6 补本业态赠送权益预估成本列）。 */
    public void upsertFormatShare(SettlementBillFormatShareRecord share) {
        jdbcTemplate.update("""
                INSERT INTO settlement_bill_format_shares (
                    bill_id, business_format, gross_revenue_cent, platform_borne_discount_cent,
                    channel_fee_cent, comp_service_fee_cent, gift_cost_cent, net_revenue_cent,
                    beneficiary_kind, beneficiary_entity_id)
                VALUES (:bill_id, :business_format, :gross, :discount, :channel_fee, :comp_service_fee,
                        :gift_cost, :net, :beneficiary_kind, :beneficiary_entity_id)
                ON CONFLICT (bill_id, business_format) DO UPDATE SET
                    gross_revenue_cent           = EXCLUDED.gross_revenue_cent,
                    platform_borne_discount_cent = EXCLUDED.platform_borne_discount_cent,
                    channel_fee_cent             = EXCLUDED.channel_fee_cent,
                    comp_service_fee_cent        = EXCLUDED.comp_service_fee_cent,
                    gift_cost_cent               = EXCLUDED.gift_cost_cent,
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

    /** 该单待过账明细：status='draft' 且 final_amount_cent>0（final=0 由调用方天然跳过，此处直接不取）。
     *  D7：带 distribution_method，供过账区分池明细(exclusive_full 除外)是否走超额 C 口径两段拆。 */
    public List<PostableItem> findPostableItems(String billId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, final_amount_cent, distribution_method
                  FROM settlement_items
                 WHERE bill_id = :bill_id AND status = 'draft' AND final_amount_cent > 0
                 ORDER BY id
                """, new MapSqlParameterSource().addValue("bill_id", billId),
                (rs, i) -> new PostableItem(rs.getString("id"), rs.getString("merchant_id"),
                        rs.getLong("final_amount_cent"), rs.getString("distribution_method")));
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

    /** 该单是否仍有未过账（draft 且 final>0）明细；无 → 可置 settled。 */
    public boolean hasUnpostedItems(String billId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM settlement_items
                 WHERE bill_id = :bill_id AND status = 'draft' AND final_amount_cent > 0
                """, new MapSqlParameterSource().addValue("bill_id", billId), Integer.class);
        return cnt != null && cnt > 0;
    }

    /**
     * S4/R-3:该单是否存在<b>负额</b>待过账明细(冲正月向商户追回)。放开带符号后负数月结算单能正常落库,
     * 但「向商户挂应收」的过账口径属过账激活批次——在那之前对负额单 fail-loud 拒过账,防止负额明细被
     * {@code final_amount_cent > 0} 过滤静默跳过、整单却被标成已结算(钱悄悄丢)。
     */
    public boolean hasNegativeDraftItems(String billId) {
        Integer cnt = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM settlement_items
                 WHERE bill_id = :bill_id AND status = 'draft' AND final_amount_cent < 0
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

    public record PostableItem(String itemId, String merchantId, long finalAmountCent, String distributionMethod) {
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
                SELECT b.status, b.project_id, b.available_pool_cent,
                       b.special_approval_valid_until,
                       (b.special_approval_ref IS NOT NULL
                        AND b.special_approval_valid_until >= CURRENT_DATE) AS special_approval_active,
                       COALESCE((SELECT SUM(i.final_amount_cent) FROM settlement_items i
                                  WHERE i.bill_id = b.id
                                    AND i.distribution_method <> 'exclusive_full'), 0) AS pooled_distributed_cent
                  FROM settlement_bills b
                 WHERE b.id = :id
                """, new MapSqlParameterSource().addValue("id", billId),
                rs -> rs.next()
                        ? new OverPoolContext(rs.getString("status"), rs.getString("project_id"),
                                rs.getLong("available_pool_cent"), rs.getLong("pooled_distributed_cent"),
                                rs.getBoolean("special_approval_active"),
                                rs.getObject("special_approval_valid_until", java.time.LocalDate.class))
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
                                  boolean specialApprovalActive, java.time.LocalDate specialApprovalValidUntil) {
        public boolean overPool() {
            return pooledDistributedCent > availablePoolCent;
        }
    }

    // ---- 读投影 ----

    /**
     * S4 财务对账读投影:月度结算单头列表(倒序,最多 limit 单)。回答财务三个问题:
     * 这单收了多少综合服务费、池链支与专属支各多少、归了谁(收款主体中文名一并带出)。
     */
    public List<BillSummary> listBills(String projectId, java.time.LocalDate periodStart, int limit) {
        return jdbcTemplate.query("""
                SELECT b.id, b.project_id, b.period_start, b.period_end,
                       b.ticket_revenue_cent, b.channel_fee_cent,
                       b.comp_service_fee_pooled_cent, b.comp_service_fee_exclusive_cent,
                       b.fee_beneficiary_entity_id, be.entity_name AS fee_beneficiary_entity_name,
                       b.available_pool_cent, b.distributed_cent, b.combo_discount_cent,
                       b.rounding_residual_cent, b.distribution_method, b.status
                  FROM settlement_bills b
                  LEFT JOIN business_entities be ON be.id = b.fee_beneficiary_entity_id
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
                        rs.getString("distribution_method"), rs.getString("status")));
    }

    /** S4 结算单头读投影(财务对账用,金额带符号:负数月=当月净追回)。 */
    public record BillSummary(String id, String projectId, java.time.LocalDate periodStart,
                              java.time.LocalDate periodEnd, long ticketRevenueCent, long channelFeeCent,
                              long compServiceFeePooledCent, long compServiceFeeExclusiveCent,
                              String feeBeneficiaryEntityId, String feeBeneficiaryEntityName,
                              long availablePoolCent, long distributedCent, long comboDiscountCent,
                              long roundingResidualCent, String distributionMethod, String status) {
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

    /** S11② 当月某销售渠道的进池核销实付额（渠道抽成整单前移的计费基数）。 */
    public record ChannelRevenue(String salesChannel, long revenueCent) {
    }
}
