package com.complex.platform.settlement;

import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算配置中心批1 写入面仓储(§2/§3/§5):8 个配置类型的落草案(Path 2 并存)+ 复核转正 + 软删 + 待复核草案聚合。
 *
 * <p><b>Path 2 落草案</b>:INSERT is_draft=TRUE,ON CONFLICT(scope 表达式, is_draft) WHERE status='active' DO UPDATE ——
 * 命中「同 scope 的草案槽」则覆盖草案,<b>不动生效行</b>(is_draft=FALSE);读侧 finder 仍读生效行 → 编辑期保值。
 * upsert 一律 RETURNING id 返回<b>真实落地行 id</b>(ON CONFLICT 命中时保留首行 id),供审计 business_id 用(§7)。
 * <p><b>Path 2 转正</b>(activateSingleRowDraft)= @Transactional 换行:旧生效行<b>软替换</b>(status→inactive,留库不物理删)
 * → 翻草案为生效;非草案/不存在 → 0 行。<p>表名 + scope 列均取自内部白名单枚举(非用户输入),无注入面。
 * <p>不写 effective_from/effective_to(见 {@link SettlementConfigWriteRequests} TODO:生效日期调度独立功能)。
 */
// ponytail: 单写入面仓储,无接口(one impl);6 张单行表用显式 upsert(payload 各异),转正走 scope 列驱动的通用换行。
@Repository
public class JdbcSettlementConfigWriteRepository {

    /** 单行 scope 配置类型 → 白名单表名 + scope 列(用于通用转正换行的「同 scope」匹配)。 */
    public enum SingleRowConfig {
        RETENTION("platform_retention_rules", "retention", List.of("project_id")),
        DISTRIBUTION_METHOD("ticket_distribution_configs", "distribution_method", List.of("project_id")),
        MERCHANT_RATE("ticket_distribution_merchant_rates", "merchant_rate", List.of("project_id", "merchant_id")),
        COMPREHENSIVE_SERVICE_FEE("comprehensive_service_fee_configs", "comprehensive_service_fee",
                List.of("project_id", "merchant_id", "entitlement_type")),
        COMBO_DISCOUNT("ticket_combo_discount_configs", "combo_discount", List.of("project_id")),
        CHANNEL_FEE("channel_fee_configs", "channel_fee", List.of("project_id", "channel"));

        private final String table;
        private final String configType;
        private final List<String> scopeColumns;

        SingleRowConfig(String table, String configType, List<String> scopeColumns) {
            this.table = table;
            this.configType = configType;
            this.scopeColumns = scopeColumns;
        }

        /** 转正泛化端点 configType(下划线)→ 白名单枚举;未知→400(不含 giveaway)。 */
        public static SingleRowConfig fromConfigType(String configType) {
            for (SingleRowConfig c : values()) {
                if (c.configType.equals(configType)) {
                    return c;
                }
            }
            throw ApiException.badRequest("UNKNOWN_CONFIG_TYPE", "未知/不支持转正的配置类型:" + configType);
        }
    }

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcSettlementConfigWriteRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ================= §2 落草案(Path 2 upsert:草案槽覆盖,不动生效行;RETURNING 真实 id)=================

    public String upsertRetentionDraft(String id, String projectId, int retentionBps, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO platform_retention_rules
                    (id, project_id, retention_bps, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :retention_bps, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    retention_bps = EXCLUDED.retention_bps,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("retention_bps", retentionBps)
                .addValue("updated_by", updatedBy), String.class);
    }

    public String upsertDistributionMethodDraft(String id, String projectId, String methodType, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ticket_distribution_configs
                    (id, project_id, method_type, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :method_type, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    method_type = EXCLUDED.method_type,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("method_type", methodType)
                .addValue("updated_by", updatedBy), String.class);
    }

    public String upsertMerchantRateDraft(String id, String projectId, String merchantId,
                                          Long perGuestCent, Long fixedAmountCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ticket_distribution_merchant_rates
                    (id, project_id, merchant_id, per_guest_cent, fixed_amount_cent, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :per_guest_cent, :fixed_amount_cent, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), merchant_id, is_draft) WHERE status = 'active' DO UPDATE SET
                    per_guest_cent = EXCLUDED.per_guest_cent,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("per_guest_cent", perGuestCent).addValue("fixed_amount_cent", fixedAmountCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    public String upsertComprehensiveServiceFeeDraft(String id, String projectId, String merchantId,
                                                     String entitlementType, int feeBps, Long fixedAmountCent,
                                                     Long monthlyCapAmountCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO comprehensive_service_fee_configs
                    (id, project_id, merchant_id, entitlement_type, fee_bps, fixed_amount_cent,
                     monthly_cap_amount_cent, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :entitlement_type, :fee_bps, :fixed_amount_cent,
                        :monthly_cap_amount_cent, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(merchant_id, '__ANY__'),
                             COALESCE(entitlement_type, '__ANY__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    fee_bps = EXCLUDED.fee_bps,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    monthly_cap_amount_cent = EXCLUDED.monthly_cap_amount_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("entitlement_type", entitlementType).addValue("fee_bps", feeBps)
                .addValue("fixed_amount_cent", fixedAmountCent)
                .addValue("monthly_cap_amount_cent", monthlyCapAmountCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    public String upsertComboDiscountDraft(String id, String projectId, String discountBearer,
                                           int splitPlatformBps, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ticket_combo_discount_configs
                    (id, project_id, discount_bearer, split_platform_bps, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :discount_bearer, :split_platform_bps, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    discount_bearer = EXCLUDED.discount_bearer,
                    split_platform_bps = EXCLUDED.split_platform_bps,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("discount_bearer", discountBearer)
                .addValue("split_platform_bps", splitPlatformBps).addValue("updated_by", updatedBy), String.class);
    }

    public String upsertChannelFeeDraft(String id, String projectId, String channel, int feeBps, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO channel_fee_configs
                    (id, project_id, channel, fee_bps, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :channel, :fee_bps, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(channel, '__ALL__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    fee_bps = EXCLUDED.fee_bps,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("channel", channel)
                .addValue("fee_bps", feeBps).addValue("updated_by", updatedBy), String.class);
    }

    /**
     * 排名档位梯子落草案(§2:@Transactional 整套替换草案梯,不动生效梯)。
     * 先删该 scope 全部草案档位,再逐行插入新草案档位(is_draft=TRUE)。草案=未提交编辑,物理删无历史价值。
     */
    @Transactional
    public void replaceRankTierDrafts(String projectId, List<TierRow> tiers, String updatedBy) {
        jdbcTemplate.update("""
                DELETE FROM ticket_distribution_rank_tiers
                 WHERE is_draft = TRUE AND status = 'active'
                   AND COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                """, new MapSqlParameterSource().addValue("project_id", projectId));
        for (TierRow t : tiers) {
            jdbcTemplate.update("""
                    INSERT INTO ticket_distribution_rank_tiers
                        (id, project_id, rank_from, rank_to, share_bps, status, is_draft, updated_by, updated_at)
                    VALUES (:id, :project_id, :rank_from, :rank_to, :share_bps, 'active', TRUE, :updated_by, now())
                    """, new MapSqlParameterSource()
                    .addValue("id", t.id()).addValue("project_id", projectId)
                    .addValue("rank_from", t.rankFrom()).addValue("rank_to", t.rankTo())
                    .addValue("share_bps", t.shareBps()).addValue("updated_by", updatedBy));
        }
    }

    // ================= §3 复核转正(旧生效行软替换 status→inactive,不物理删)=================

    /**
     * 单行 scope 类型转正(Path 2 换行,@Transactional):
     * ① 旧生效行(is_draft=FALSE, status=active)软替换 status→inactive(留库);② 翻草案行为生效;③ 返回②行数(0=非草案/不存在)。
     * scope「同一」用 null 安全等价 IS NOT DISTINCT FROM(免 COALESCE 哨兵);表名+scope 列来自白名单枚举。
     */
    @Transactional
    public int activateSingleRowDraft(SingleRowConfig cfg, String id, String updatedBy, Instant now) {
        StringBuilder scopeEq = new StringBuilder();
        for (String col : cfg.scopeColumns) {
            scopeEq.append(" AND l.").append(col).append(" IS NOT DISTINCT FROM d.").append(col);
        }
        // ① 旧生效行软替换 → inactive(仅 active 生效行;已 inactive 历史行不动)
        jdbcTemplate.update("UPDATE " + cfg.table + " l"
                + " SET status = 'inactive', updated_by = :updated_by, updated_at = :now"
                + " FROM " + cfg.table + " d"
                + " WHERE d.id = :id AND d.is_draft = TRUE AND d.status = 'active'"
                + " AND l.is_draft = FALSE AND l.status = 'active' AND l.id <> d.id" + scopeEq,
                new MapSqlParameterSource().addValue("id", id)
                        .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
        // ② 翻草案为生效
        return jdbcTemplate.update("UPDATE " + cfg.table
                + " SET is_draft = FALSE, updated_by = :updated_by, updated_at = :now"
                + " WHERE id = :id AND is_draft = TRUE AND status = 'active'",
                new MapSqlParameterSource().addValue("id", id)
                        .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    /**
     * 排名档位梯子整体转正(§3:@Transactional 换梯):旧生效梯软替换 status→inactive → 翻草案梯为生效。返回翻转行数(0=无草案梯)。
     */
    @Transactional
    public int activateRankTierDrafts(String projectId, String updatedBy, Instant now) {
        // ① 旧生效梯软替换 → inactive;EXISTS 守卫:仅当存在待转正草案梯才停用(对齐 activateSingleRowDraft 自连接,
        //    根除"无草案却把生效梯停用致 by_rank_tier 0 分账"——服务层空草案守卫 + @Transactional 之外的第三重 DB 级纵深)。
        jdbcTemplate.update("""
                UPDATE ticket_distribution_rank_tiers
                   SET status = 'inactive', updated_by = :updated_by, updated_at = :now
                 WHERE is_draft = FALSE AND status = 'active'
                   AND COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                   AND EXISTS (SELECT 1 FROM ticket_distribution_rank_tiers d
                                WHERE d.is_draft = TRUE AND d.status = 'active'
                                  AND COALESCE(d.project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__'))
                """, new MapSqlParameterSource().addValue("project_id", projectId)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
        return jdbcTemplate.update("""
                UPDATE ticket_distribution_rank_tiers
                   SET is_draft = FALSE, updated_by = :updated_by, updated_at = :now
                 WHERE is_draft = TRUE AND status = 'active'
                   AND COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                """, new MapSqlParameterSource().addValue("project_id", projectId)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    // ================= §5 差人转正:草案起草人(updated_by)读取 =================

    /** 单行草案起草人(不存在/非草案返 null)。 */
    public String singleRowDraftUpdatedBy(SingleRowConfig cfg, String id) {
        return jdbcTemplate.query("SELECT updated_by FROM " + cfg.table
                + " WHERE id = :id AND is_draft = TRUE AND status = 'active'",
                new MapSqlParameterSource().addValue("id", id),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    /** 排名档位草案起草人(取该 project 任一草案档 updated_by;无草案返 null)。 */
    public String rankTierDraftUpdatedBy(String projectId) {
        return jdbcTemplate.query("""
                SELECT updated_by FROM ticket_distribution_rank_tiers
                 WHERE is_draft = TRUE AND status = 'active'
                   AND COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                 LIMIT 1
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    /** merchant-rate active 行最后编辑人(供软删差人守卫;不存在返 null)。 */
    public String merchantRateActiveUpdatedBy(String id) {
        return jdbcTemplate.query(
                "SELECT updated_by FROM ticket_distribution_merchant_rates WHERE id = :id AND status = 'active'",
                new MapSqlParameterSource().addValue("id", id),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    /** 读该 project 的草案档位(供转正前区间覆盖复校,§2)。 */
    public List<TierRow> listDraftRankTiers(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, rank_from, rank_to, share_bps FROM ticket_distribution_rank_tiers
                 WHERE is_draft = TRUE AND status = 'active'
                   AND COALESCE(project_id, '__DEFAULT__') = COALESCE(:project_id, '__DEFAULT__')
                 ORDER BY rank_from
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                (rs, i) -> new TierRow(rs.getString("id"), rs.getInt("rank_from"),
                        rs.getInt("rank_to"), rs.getInt("share_bps")));
    }

    // ================= §2 软删(每客单价/固定额条目;归属条目软删在 RevenueStreamAttributionRepository)=================

    /** merchant-rate 软删铁律:status→inactive + archive_reason(仅 active 行)。返回更新行数。 */
    public int softDeleteMerchantRate(String id, String archiveReason, String updatedBy, Instant now) {
        return jdbcTemplate.update("""
                UPDATE ticket_distribution_merchant_rates
                   SET status = 'inactive', archive_reason = :archive_reason,
                       updated_by = :updated_by, updated_at = :now
                 WHERE id = :id AND status = 'active'
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("archive_reason", archiveReason)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    // ================= §5 待复核草案聚合(各配置表 is_draft=TRUE AND status='active' 的 UNION)=================

    /** 全部结算配置表的待复核草案(供 overview 草案队列)。giveaway(废)/settlement_tax(零消费)不入。 */
    public List<PendingConfigDraft> listPendingConfigDrafts() {
        return jdbcTemplate.query("""
                SELECT id, config_group, summary FROM (
                    SELECT id, 'retention' AS config_group,
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 留存 ' || retention_bps || 'bps' AS summary,
                           updated_at
                      FROM platform_retention_rules WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'distribution_method',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 分配方式 ' || method_type,
                           updated_at
                      FROM ticket_distribution_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'merchant_rate',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 商户=' || merchant_id
                               || ' 每客/固定=' || COALESCE(per_guest_cent::text, '-') || '/' || COALESCE(fixed_amount_cent::text, '-'),
                           updated_at
                      FROM ticket_distribution_merchant_rates WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'comprehensive_service_fee',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 商户=' || COALESCE(merchant_id, '__ANY__')
                               || ' 票种=' || COALESCE(entitlement_type, '__ANY__')
                               || ' 费=' || CASE WHEN fixed_amount_cent IS NULL THEN fee_bps || 'bps' ELSE fixed_amount_cent || '分' END
                               || CASE WHEN monthly_cap_amount_cent IS NULL THEN '' ELSE ' 封顶' || monthly_cap_amount_cent || '分' END,
                           updated_at
                      FROM comprehensive_service_fee_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'combo_discount',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 折扣归属 ' || discount_bearer,
                           updated_at
                      FROM ticket_combo_discount_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'channel_fee',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 渠道=' || COALESCE(channel, '__ALL__')
                               || ' 抽成 ' || fee_bps || 'bps',
                           updated_at
                      FROM channel_fee_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT MIN(id) AS id, 'rank_tiers',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 排名档位草案(' || COUNT(*) || ' 档)',
                           MAX(updated_at) AS updated_at
                      FROM ticket_distribution_rank_tiers WHERE is_draft = TRUE AND status = 'active'
                      GROUP BY project_id
                    UNION ALL
                    SELECT id, 'revenue_stream_attribution',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' ' || revenue_stream || ' → ' || management_entity_id,
                           updated_at
                      FROM revenue_stream_attribution WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'attribution_rule',
                           rule_scope || ' / ' || revenue_source || ' · ' || attribution_mode || ' · ' || share_bps || 'bps',
                           updated_at
                      FROM merchant_revenue_attribution_rules WHERE is_draft = TRUE AND status = 'active'
                ) q
                ORDER BY updated_at DESC NULLS LAST, config_group, id
                """, new MapSqlParameterSource(), (rs, i) ->
                new PendingConfigDraft(rs.getString("id"), rs.getString("config_group"), rs.getString("summary")));
    }

    /** 排名档位一行(落草案 / 读草案用)。 */
    public record TierRow(String id, int rankFrom, int rankTo, int shareBps) {
    }

    /** 待复核草案条目(供 overview 展示)。 */
    public record PendingConfigDraft(String id, String configGroup, String summary) {
    }
}
