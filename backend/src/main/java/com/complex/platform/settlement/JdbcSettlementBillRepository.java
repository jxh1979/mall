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
public class JdbcSettlementBillRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSettlementBillRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ---- 读配置（COALESCE 默认+项目特例；无配置回退建表默认值）----

    /** 留存万分比：项目特例优先，否则全局默认，否则建表默认 2000（20%）。 */
    public int findRetentionBps(String projectId) {
        Integer bps = jdbcTemplate.query("""
                SELECT retention_bps FROM platform_retention_rules
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                rs -> rs.next() ? rs.getInt("retention_bps") : null);
        return requireConfigured(bps, "platform_retention_rules.retention_bps", projectId);
    }

    /** 第三方渠道抽成万分比：项目特例优先，否则全局默认，否则建表默认 0（不抽）。批1 只取通用率（channel IS NULL），per-channel 批6 用。 */
    public int findChannelFeeBps(String projectId) {
        Integer bps = jdbcTemplate.query("""
                SELECT fee_bps FROM channel_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE AND channel IS NULL
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
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

    /** 某商户的每客单价/固定额费率：项目特例优先，否则全局默认（同商户）。 */
    public MerchantRate findMerchantRate(String projectId, String merchantId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("project_id", projectId).addValue("merchant_id", merchantId);
        return jdbcTemplate.query("""
                SELECT per_guest_cent, fixed_amount_cent FROM ticket_distribution_merchant_rates
                 WHERE status = 'active' AND is_draft = FALSE AND merchant_id = :merchant_id
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, params, rs -> rs.next()
                ? new MerchantRate(
                        rs.getObject("per_guest_cent") == null ? null : rs.getLong("per_guest_cent"),
                        rs.getObject("fixed_amount_cent") == null ? null : rs.getLong("fixed_amount_cent"))
                : null);
    }

    // ---- 读当月核销聚合（批1 settlement_transactions，verification_settled，按 created_at 落窗）----

    /**
     * 当月<b>进池</b>通票收入（已核销）= Σ verified_amount_cent，<b>仅非专属</b>（exclusive_merchant_id IS NULL）。
     * 专属某商户核销全额直付该商户，不进池（批3-4 §6.4）。
     * 月窗 [monthStart, nextMonthStart) 按 created_at（批1 双写时已填=核销记录创建≈核销日，语义正确；
     * confirmed_at 是结算确认时间、双写时本应 NULL 故不用）；仅 verification_settled。
     */
    public long sumVerifiedRevenueCent(String projectId, Instant monthStart, Instant nextMonthStart) {
        // AUD-012/013：核销增量 + 冲正负向，按 business_at(核销/冲正业务时刻)落月窗，冲正对配天然净掉。
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(verified_amount_cent), 0)
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND exclusive_merchant_id IS NULL
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                """, monthWindow(projectId, monthStart, nextMonthStart), Long.class);
        return sum == null ? 0L : sum;
    }

    /** 当月各酒吧统计：按 merchant_id 聚合 核销数(Σverified_count) + 客数(Σnum_people)。仅归属商户的核销。 */
    public List<MerchantMonthlyStat> aggregateMerchantStats(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.query("""
                SELECT merchant_id,
                       COALESCE(SUM(verified_count), 0) AS verified_count,
                       COALESCE(SUM(num_people), 0)     AS guest_count
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND beneficiary_type = 'merchant'
                   AND merchant_id IS NOT NULL
                   AND exclusive_merchant_id IS NULL
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                 GROUP BY merchant_id
                 ORDER BY verified_count DESC, merchant_id
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) ->
                new MerchantMonthlyStat(rs.getString("merchant_id"),
                        rs.getInt("verified_count"), rs.getInt("guest_count")));
    }

    /**
     * 结算重构批3-4(§6.4)：当月<b>专属</b>核销按 exclusive_merchant_id 分组 Σ verified_amount_cent。
     * 每个专属商户一条 → 全额直付明细（method=exclusive_full，不扣留存、不进 spvPool、不稀释他家分池）。
     * 按落定在 settlement_transactions 的 exclusive_merchant_id 过滤（非 join 活模板，历史结算不受改模板影响）。
     */
    public List<ExclusiveMerchantRevenue> aggregateExclusiveRevenue(String projectId, Instant monthStart, Instant nextMonthStart) {
        return jdbcTemplate.query("""
                SELECT exclusive_merchant_id AS merchant_id,
                       entitlement_type,
                       COALESCE(SUM(verified_amount_cent), 0) AS revenue_cent,
                       COALESCE(SUM(discount_amount_cent), 0) AS discount_cent,
                       COALESCE(SUM(verified_count), 0)       AS verified_count,
                       COALESCE(SUM(num_people), 0)           AS guest_count
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND exclusive_merchant_id IS NOT NULL
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                 GROUP BY exclusive_merchant_id, entitlement_type
                 ORDER BY exclusive_merchant_id, entitlement_type
                """, monthWindow(projectId, monthStart, nextMonthStart), (rs, i) ->
                new ExclusiveMerchantRevenue(rs.getString("merchant_id"), rs.getString("entitlement_type"),
                        rs.getLong("revenue_cent"), rs.getLong("discount_cent"),
                        rs.getInt("verified_count"), rs.getInt("guest_count")));
    }

    /**
     * D6 当月<b>非专属</b>（进池）核销的套票折扣额合计 = Σ discount_amount_cent（核销正 + 冲正负，配对净掉）。
     * 供分账引擎在 platform_absorb/split_by_ratio 时把项目承担的折扣加进池基数（池按满额分）。
     * merchant_absorb 时引擎不加此项 → 商户自担折扣（池按净额分）=现状。为空/无折扣 → 0（零行为变更）。
     */
    public long sumComboDiscountCent(String projectId, Instant monthStart, Instant nextMonthStart) {
        Long sum = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(discount_amount_cent), 0)
                  FROM settlement_transactions
                 WHERE transaction_type IN ('verification_settled','verification_reversed')
                   AND exclusive_merchant_id IS NULL
                   AND COALESCE(business_at, created_at) >= :start AND COALESCE(business_at, created_at) < :end
                   AND (:project_id::text IS NULL OR project_id = :project_id)
                """, monthWindow(projectId, monthStart, nextMonthStart), Long.class);
        return sum == null ? 0L : sum;
    }

    private MapSqlParameterSource monthWindow(String projectId, Instant monthStart, Instant nextMonthStart) {
        return new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("start", Timestamp.from(monthStart))
                .addValue("end", Timestamp.from(nextMonthStart));
    }

    /**
     * V268 综合服务费(§7.2 口径8+"所有门票都抽")万分比:项目x商户x票种范围可配,默认 0(不抽)。
     * 查询精度:商户+票种特例 > 商户特例 > 票种特例 > 项目默认(COALESCE 归一 NULL,ORDER 让更具体的优先)。
     * 无任何配置行 -> 回退 0(禁硬编码,与建表默认一致) -> 该笔专属直付零扣费,商户拿全额,零行为变更。
     */
    public ServiceFeeConfig findComprehensiveServiceFee(String projectId, String merchantId, String entitlementType) {
        ServiceFeeConfig cfg = jdbcTemplate.query("""
                SELECT fee_bps, fixed_amount_cent FROM comprehensive_service_fee_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                   AND (merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (entitlement_type IS NULL OR entitlement_type = :entitlement_type)
                 ORDER BY (project_id IS NOT NULL) DESC,
                          (merchant_id IS NOT NULL) DESC,
                          (entitlement_type IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("merchant_id", merchantId)
                .addValue("entitlement_type", entitlementType),
                rs -> rs.next()
                        ? new ServiceFeeConfig(rs.getInt("fee_bps"),
                                rs.getObject("fixed_amount_cent") == null ? null : rs.getLong("fixed_amount_cent"))
                        : null);
        return requireConfigured(cfg, "comprehensive_service_fee_configs", projectId);
    }

    /**
     * V268 套票折扣归属(§7.4 口径10):项目级可配,默认 platform_absorb(项目承担)。
     * 说明:当前无折扣额进结算引擎(折扣是定价期概念),本解析器先落地供后台/引擎读,逐笔应用待折扣额接入流水后启用。
     */
    public ComboDiscountConfig findComboDiscountConfig(String projectId) {
        ComboDiscountConfig cfg = jdbcTemplate.query("""
                SELECT discount_bearer, split_platform_bps FROM ticket_combo_discount_configs
                 WHERE status = 'active' AND is_draft = FALSE
                   AND COALESCE(project_id, '__DEFAULT__') IN (COALESCE(:project_id, '__DEFAULT__'), '__DEFAULT__')
                 ORDER BY (project_id IS NOT NULL) DESC
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                rs -> rs.next()
                        ? new ComboDiscountConfig(rs.getString("discount_bearer"), rs.getInt("split_platform_bps"))
                        : null);
        return requireConfigured(cfg, "ticket_combo_discount_configs", projectId);
    }

    /**
     * §4 回退常量收敛为 fail-loud:结算配置缺失(连 __DEFAULT__ 种子都没有=部署坏)时【响亮停】而非静默套硬编码默认。
     * 正常态 V223/V268/V327 恒建 __DEFAULT__ 行 + V326 已转正 is_draft=FALSE,故 5 个 finder 恒命中,本助手不触发,零行为变更;
     * null 只在种子被删/迁移未跑到位时出现——此时报错正是「零硬编码·无配置即响亮停」铁则要的。
     * (findMerchantRate/findRankTiers 不收敛:前者合法返 null 引擎自处置,后者空梯子给 0 分。)
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
                .addValue("giveaway_cost_cent", bill.giveawayCostCent())
                .addValue("available_pool_cent", bill.availablePoolCent())
                .addValue("retention_bps", bill.retentionBps())
                .addValue("retention_cent", bill.retentionCent())
                .addValue("spv_pool_cent", bill.spvPoolCent())
                .addValue("distributed_cent", bill.distributedCent())
                .addValue("comp_service_fee_cent", bill.compServiceFeeCent())
                .addValue("distribution_method", bill.distributionMethod())
                .addValue("special_approval_ref", bill.specialApprovalRef())
                .addValue("status", bill.status())
                .addValue("attributed_entity_id", bill.attributedEntityId())
                .addValue("combo_discount_cent", bill.comboDiscountCent());
        jdbcTemplate.update("""
                INSERT INTO settlement_bills (
                    id, project_id, period_start, period_end, ticket_revenue_cent, channel_fee_cent,
                    giveaway_cost_cent, available_pool_cent, retention_bps, retention_cent, spv_pool_cent,
                    distributed_cent, comp_service_fee_cent, distribution_method, special_approval_ref, status,
                    attributed_entity_id, combo_discount_cent, updated_at
                )
                VALUES (
                    :id, :project_id, :period_start, :period_end, :ticket_revenue_cent, :channel_fee_cent,
                    :giveaway_cost_cent, :available_pool_cent, :retention_bps, :retention_cent, :spv_pool_cent,
                    :distributed_cent, :comp_service_fee_cent, :distribution_method, :special_approval_ref, :status,
                    :attributed_entity_id, :combo_discount_cent, now()
                )
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), period_start, period_end) DO UPDATE SET
                    ticket_revenue_cent = EXCLUDED.ticket_revenue_cent,
                    channel_fee_cent    = EXCLUDED.channel_fee_cent,
                    giveaway_cost_cent  = EXCLUDED.giveaway_cost_cent,
                    available_pool_cent = EXCLUDED.available_pool_cent,
                    retention_bps       = EXCLUDED.retention_bps,
                    retention_cent      = EXCLUDED.retention_cent,
                    spv_pool_cent       = EXCLUDED.spv_pool_cent,
                    distributed_cent    = EXCLUDED.distributed_cent,
                    comp_service_fee_cent = EXCLUDED.comp_service_fee_cent,
                    distribution_method = EXCLUDED.distribution_method,
                    special_approval_ref = EXCLUDED.special_approval_ref,
                    status              = EXCLUDED.status,
                    attributed_entity_id = EXCLUDED.attributed_entity_id,
                    combo_discount_cent = EXCLUDED.combo_discount_cent,
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

    /** 该单待过账明细：status='draft' 且 final_amount_cent>0（final=0 由调用方天然跳过，此处直接不取）。 */
    public List<PostableItem> findPostableItems(String billId) {
        return jdbcTemplate.query("""
                SELECT id, merchant_id, final_amount_cent
                  FROM settlement_items
                 WHERE bill_id = :bill_id AND status = 'draft' AND final_amount_cent > 0
                 ORDER BY id
                """, new MapSqlParameterSource().addValue("bill_id", billId),
                (rs, i) -> new PostableItem(rs.getString("id"), rs.getString("merchant_id"),
                        rs.getLong("final_amount_cent")));
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

    /** 置结算单 status='settled' + settled_at=now（仅当当前 confirmed，幂等：重复过账不重置已 settled 单）。 */
    public int markBillSettled(String billId) {
        return jdbcTemplate.update("""
                UPDATE settlement_bills
                   SET status = 'settled', settled_at = now(), updated_at = now()
                 WHERE id = :id AND status = 'confirmed'
                """, new MapSqlParameterSource().addValue("id", billId));
    }

    public record PostableItem(String itemId, String merchantId, long finalAmountCent) {
    }

    // ---- 读投影 ----

    /** V268 套票折扣归属配置读投影(§7.4)。splitPlatformBps 仅 split_by_ratio 用。 */
    public record ComboDiscountConfig(String discountBearer, int splitPlatformBps) {
    }

    /** V268 综合服务费配置读投影(§7.2):fixedAmountCent 非空=用固定额,否则用 feeBps 比例。 */
    public record ServiceFeeConfig(int feeBps, Long fixedAmountCent) {
    }

    public record RankTier(int rankFrom, int rankTo, int shareBps) {
    }

    public record MerchantRate(Long perGuestCent, Long fixedAmountCent) {
    }

    public record MerchantMonthlyStat(String merchantId, int verifiedCount, int guestCount) {
    }

    /** 批3-4 专属核销直付投影：某专属商户x票种当月全额收入 + 核销数/客数（仅供明细展示，不进池排名）。
     *  V268 加 entitlementType：综合服务费(§7.2)按 项目x商户x票种范围 查费率,故按票种拆聚合。
     *  D6 加 discountCent：该专属商户x票种当月套票折扣额合计（供引擎按归属把项目承担部分补回商户净额）。 */
    public record ExclusiveMerchantRevenue(String merchantId, String entitlementType, long revenueCent,
                                           long discountCent, int verifiedCount, int guestCount) {
    }
}
