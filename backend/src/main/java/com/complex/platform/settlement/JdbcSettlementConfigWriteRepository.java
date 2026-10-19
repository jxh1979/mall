package com.complex.platform.settlement;

// 文件版本：V0.03

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.LocalZone;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算配置中心批1 写入面仓储(§2/§3/§5):配置类型的落草案(Path 2 并存)+ 复核转正 + 软删 + 待复核草案聚合。
 * 批1 收 8 类;V340(P0-D/CFG-01)追加「卡座提成费率 / 积分累计率」2 类,共 10 类。
 *
 * <p><b>Path 2 落草案</b>:INSERT is_draft=TRUE,ON CONFLICT(scope 表达式, is_draft) WHERE status='active' DO UPDATE ——
 * 命中「同 scope 的草案槽」则覆盖草案,<b>不动生效行</b>(is_draft=FALSE);读侧 finder 仍读生效行 → 编辑期保值。
 * upsert 一律 RETURNING id 返回<b>真实落地行 id</b>(ON CONFLICT 命中时保留首行 id),供审计 business_id 用(§7)。
 * <p><b>Path 2 转正</b>(activateSingleRowDraft)= @Transactional 换行:旧生效行<b>软替换</b>(status→inactive,留库不物理删)
 * → 翻草案为生效;非草案/不存在 → 0 行。<p>表名 + scope 列均取自内部白名单枚举(非用户输入),无注入面。
 * <p>抽成规则变更按批准所在地月份的次月首日生效；初始规则保留合同起始日期，其他配置日期行为不变。
 */
// ponytail: 单写入面仓储,无接口(one impl);6 张单行表用显式 upsert(payload 各异),转正走 scope 列驱动的通用换行。
@Repository
public class JdbcSettlementConfigWriteRepository {

    /** 单行 scope 配置类型 → 白名单表名 + scope 列(用于通用转正换行的「同 scope」匹配)。 */
    public enum SingleRowConfig {
        // S4:原 RETENTION(platform_retention_rules)已下线——留存与门票综合服务费为同一项,表随 V370 停用。
        DISTRIBUTION_METHOD("ticket_distribution_configs", "distribution_method", List.of("project_id")),
        MERCHANT_RATE("ticket_distribution_merchant_rates", "merchant_rate", List.of("project_id", "merchant_id")),
        // S2(V365):第三维换成票种(原权益类型)——业务人员按票种谈费率、按票种对账。
        COMPREHENSIVE_SERVICE_FEE("comprehensive_service_fee_configs", "comprehensive_service_fee",
                List.of("project_id", "merchant_id", "ticket_type_id")),
        COMBO_DISCOUNT("ticket_combo_discount_configs", "combo_discount", List.of("project_id")),
        CHANNEL_FEE("channel_fee_configs", "channel_fee", List.of("project_id", "channel")),
        // V340（P0-D/CFG-01）：提成费率按 项目 × 商户 × 角色档；积分累计率按 项目 × 商户。同 Path 2 草案/转正范式。
        COMMISSION_RATE("bar_commission_rate_configs", "commission_rate",
                List.of("project_id", "merchant_id", "role_type")),
        POINTS_ACCRUAL_RATE("points_accrual_rate_configs", "points_accrual_rate",
                List.of("project_id", "merchant_id")),
        // V350：全平台「高价值物品」判定阈值（退酒/报损/盘点差异/存酒作废共用）按 项目 × 商户。同 Path 2 草案/转正范式。
        HIGH_VALUE_THRESHOLD("high_value_thresholds", "high_value_threshold",
                List.of("project_id", "merchant_id")),
        // V346（P0-E）：营业额抽成规则收进配置中心草案闸。scope = 合同 × 商户
        //（同合同同商户的旧生效规则在转正时软替换；写面在 FinanceService.createTurnoverRentRule，本处只做转正/软删）。
        TURNOVER_RENT_RULE("turnover_rent_rules", "turnover_rent_rule",
                List.of("contract_id", "merchant_id")),
        // S6（V373）：赠送权益预扣规则按 项目 × 票种（留空=该层通用）。同 Path 2 草案/转正范式。
        PASS_GIFT_COST("pass_gift_cost_configs", "pass_gift_cost",
                List.of("project_id", "ticket_type_id")),
        // 批10/E（V379）：内部结算 SKU 目录条目。scope = item_code（条目稳定业务键，业务表引用它）；
        // 同 code 旧生效行在转正时软替换 inactive（留库即价格版本史）。转正后的快照传播见
        // SettlementConfigWriteService.activateDraft 专案分支（propagateCatalogPriceToSnapshots）。
        INTERNAL_PRICE_ITEM("internal_price_catalog", "internal_price_item",
                List.of("item_code"));

        private final String table;
        private final String configType;
        private final List<String> scopeColumns;

        SingleRowConfig(String table, String configType, List<String> scopeColumns) {
            this.table = table;
            this.configType = configType;
            this.scopeColumns = scopeColumns;
        }

        /** 白名单表名(供 V340 软删/审计取表名;非用户输入)。 */
        public String table() {
            return table;
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

    public String upsertDistributionMethodDraft(String id, String projectId, String methodType,
                                                int specialApprovalTermValue, String specialApprovalTermUnit,
                                                String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ticket_distribution_configs
                    (id, project_id, method_type, special_approval_term_value, special_approval_term_unit,
                     status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :method_type, :term_value, :term_unit, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    method_type = EXCLUDED.method_type,
                    special_approval_term_value = EXCLUDED.special_approval_term_value,
                    special_approval_term_unit = EXCLUDED.special_approval_term_unit,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("method_type", methodType)
                .addValue("term_value", specialApprovalTermValue).addValue("term_unit", specialApprovalTermUnit)
                .addValue("updated_by", updatedBy), String.class);
    }

    public String upsertMerchantRateDraft(String id, String projectId, String merchantId, String ruleType,
                                          Long perGuestCent, Long fixedAmountCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ticket_distribution_merchant_rates
                    (id, project_id, merchant_id, rule_type, per_guest_cent, fixed_amount_cent, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :rule_type, :per_guest_cent, :fixed_amount_cent, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), merchant_id, is_draft) WHERE status = 'active' DO UPDATE SET
                    rule_type = EXCLUDED.rule_type,
                    per_guest_cent = EXCLUDED.per_guest_cent,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("rule_type", ruleType)
                .addValue("per_guest_cent", perGuestCent).addValue("fixed_amount_cent", fixedAmountCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    public String upsertComprehensiveServiceFeeDraft(String id, String projectId, String merchantId,
                                                     String ticketTypeId, int feeBps, Long fixedAmountCent,
                                                     Long monthlyCapAmountCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO comprehensive_service_fee_configs
                    (id, project_id, merchant_id, ticket_type_id, fee_bps, fixed_amount_cent,
                     monthly_cap_amount_cent, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :ticket_type_id, :fee_bps, :fixed_amount_cent,
                        :monthly_cap_amount_cent, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(merchant_id, '__ANY__'),
                             COALESCE(ticket_type_id, '__ANY__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    fee_bps = EXCLUDED.fee_bps,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    monthly_cap_amount_cent = EXCLUDED.monthly_cap_amount_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("ticket_type_id", ticketTypeId).addValue("fee_bps", feeBps)
                .addValue("fixed_amount_cent", fixedAmountCent)
                .addValue("monthly_cap_amount_cent", monthlyCapAmountCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    /** S2:票种存在性(供综合服务费费率规则写入前校验,把坏 id 拦成中文 400 而不是撞 FK 抛 500)。 */
    public boolean ticketTypeExists(String ticketTypeId) {
        Integer hit = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ticket_types WHERE id = :id",
                new MapSqlParameterSource().addValue("id", ticketTypeId), Integer.class);
        return hit != null && hit > 0;
    }

    /** V369:同一行再带「凑整允许误差」(分)——凑整差额走的就是这条折扣承担口径,运营只在一处维护。 */
    public String upsertComboDiscountDraft(String id, String projectId, String discountBearer,
                                           int splitPlatformBps, long roundingToleranceCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ticket_combo_discount_configs
                    (id, project_id, discount_bearer, split_platform_bps, rounding_tolerance_cent,
                     status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :discount_bearer, :split_platform_bps, :rounding_tolerance_cent,
                     'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    discount_bearer = EXCLUDED.discount_bearer,
                    split_platform_bps = EXCLUDED.split_platform_bps,
                    rounding_tolerance_cent = EXCLUDED.rounding_tolerance_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("discount_bearer", discountBearer)
                .addValue("split_platform_bps", splitPlatformBps)
                .addValue("rounding_tolerance_cent", roundingToleranceCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    /** V381:同一草案槽再带「每张票固定金额」(分,与 fee_bps 二选一,写入面与 chk_cfc_fee_mode 双闸)。 */
    public String upsertChannelFeeDraft(String id, String projectId, String channel, int feeBps,
                                        Long fixedAmountCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO channel_fee_configs
                    (id, project_id, channel, fee_bps, fixed_amount_cent, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :channel, :fee_bps, :fixed_amount_cent, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(channel, '__ALL__'), is_draft) WHERE status = 'active' DO UPDATE SET
                    fee_bps = EXCLUDED.fee_bps,
                    fixed_amount_cent = EXCLUDED.fixed_amount_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("channel", channel)
                .addValue("fee_bps", feeBps).addValue("fixed_amount_cent", fixedAmountCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    /** V340 卡座提成费率落草案(项目 × 商户 × 角色档;merchant_id NULL=该项目通用)。 */
    public String upsertCommissionRateDraft(String id, String projectId, String merchantId,
                                            String roleType, int rateBp, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO bar_commission_rate_configs
                    (id, project_id, merchant_id, role_type, rate_bp, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :role_type, :rate_bp, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(merchant_id, '__ANY__'),
                             role_type, is_draft) WHERE status = 'active' DO UPDATE SET
                    rate_bp = EXCLUDED.rate_bp,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("role_type", roleType).addValue("rate_bp", rateBp)
                .addValue("updated_by", updatedBy), String.class);
    }

    /** V340 积分累计率落草案(项目 × 商户;amount_cent_per_point=每积 1 分所需消费金额,分)。 */
    public String upsertPointsAccrualRateDraft(String id, String projectId, String merchantId,
                                               long amountCentPerPoint, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO points_accrual_rate_configs
                    (id, project_id, merchant_id, amount_cent_per_point, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :amount_cent_per_point, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(merchant_id, '__ANY__'),
                             is_draft) WHERE status = 'active' DO UPDATE SET
                    amount_cent_per_point = EXCLUDED.amount_cent_per_point,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("amount_cent_per_point", amountCentPerPoint)
                .addValue("updated_by", updatedBy), String.class);
    }

    /**
     * S6(V373)赠送权益预扣规则落草案(项目 × 票种;两留空=平台默认)。两模式二选一:
     * by_item 按件计(整额必 NULL,票面推导);by_ticket_amount 按票计(整额必填 ≥0)——服务层已校验,DB CHECK 兜底。
     */
    public String upsertPassGiftCostDraft(String id, String projectId, String ticketTypeId,
                                          String predeductMode, Long ticketGiftAmountCent,
                                          Long monthlyCapAmountCent, String varianceHandling, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO pass_gift_cost_configs
                    (id, project_id, ticket_type_id, prededuct_mode, ticket_gift_amount_cent,
                     monthly_cap_amount_cent, variance_handling, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :ticket_type_id, :prededuct_mode, :ticket_gift_amount_cent,
                     :monthly_cap_amount_cent, :variance_handling, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(ticket_type_id, '__ANY__'),
                             is_draft) WHERE status = 'active' DO UPDATE SET
                    prededuct_mode = EXCLUDED.prededuct_mode,
                    ticket_gift_amount_cent = EXCLUDED.ticket_gift_amount_cent,
                    monthly_cap_amount_cent = EXCLUDED.monthly_cap_amount_cent,
                    variance_handling = EXCLUDED.variance_handling,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("ticket_type_id", ticketTypeId)
                .addValue("prededuct_mode", predeductMode)
                .addValue("ticket_gift_amount_cent", ticketGiftAmountCent)
                .addValue("monthly_cap_amount_cent", monthlyCapAmountCent)
                .addValue("variance_handling", varianceHandling)
                .addValue("updated_by", updatedBy), String.class);
    }

    /** V350 全平台高价值判定阈值落草案（项目 × 商户；merchant_id NULL=该项目通用）。单位：分，比的是物品进货价。 */
    public String upsertHighValueThresholdDraft(String id, String projectId, String merchantId,
                                              long highValueThresholdCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO high_value_thresholds
                    (id, project_id, merchant_id, high_value_threshold_cent, status, is_draft, updated_by, updated_at)
                VALUES (:id, :project_id, :merchant_id, :high_value_threshold_cent, 'active', TRUE, :updated_by, now())
                ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), COALESCE(merchant_id, '__ANY__'),
                             is_draft) WHERE status = 'active' DO UPDATE SET
                    high_value_threshold_cent = EXCLUDED.high_value_threshold_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("project_id", projectId).addValue("merchant_id", merchantId)
                .addValue("high_value_threshold_cent", highValueThresholdCent)
                .addValue("updated_by", updatedBy), String.class);
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

    // ================= 批10/E 内部结算 SKU 目录(V379/V380) =================

    /** 目录条目落草案(Path 2:草案槽覆盖,不动生效行)。新建与改价同一入口:item_code 新码=新建,既有码=改价草案。 */
    public String upsertInternalPriceItemDraft(String id, String itemCode, String merchantId, String category,
                                               String itemName, long priceCent, String updatedBy) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO internal_price_catalog
                    (id, item_code, merchant_id, category, item_name, price_cent, status, is_draft, updated_by,
                     created_at, updated_at)
                VALUES (:id, :item_code, :merchant_id, :category, :item_name, :price_cent, 'active', TRUE, :updated_by,
                        now(), now())
                ON CONFLICT (item_code, is_draft) WHERE status = 'active' DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    category = EXCLUDED.category,
                    item_name = EXCLUDED.item_name,
                    price_cent = EXCLUDED.price_cent,
                    updated_by = EXCLUDED.updated_by,
                    updated_at = now()
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("id", id).addValue("item_code", itemCode).addValue("merchant_id", merchantId)
                .addValue("category", category).addValue("item_name", itemName).addValue("price_cent", priceCent)
                .addValue("updated_by", updatedBy), String.class);
    }

    /** 生效条目(is_draft=FALSE,status=active)读取:引用校验/快照同步用。 */
    public CatalogEntryRow findActiveCatalogEntry(String itemCode) {
        List<CatalogEntryRow> rows = jdbcTemplate.query("""
                SELECT c.id, c.item_code, c.merchant_id, m.merchant_type AS business_format, c.category, c.item_name,
                       c.price_cent, c.status, c.is_draft, c.updated_by, c.updated_at
                  FROM internal_price_catalog c
                  LEFT JOIN merchants m ON m.id = c.merchant_id
                 WHERE c.item_code = :item_code AND c.is_draft = FALSE AND c.status = 'active'
                """, new MapSqlParameterSource().addValue("item_code", itemCode), CATALOG_ROW_MAPPER);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 同 code 生效或草案行是否已被别的商户占用(引用/新建守卫:一码一商家,不得跨商户复用)。 */
    public String catalogItemMerchant(String itemCode) {
        return jdbcTemplate.query("""
                SELECT merchant_id FROM internal_price_catalog
                 WHERE item_code = :item_code AND status = 'active'
                 ORDER BY is_draft LIMIT 1
                """, new MapSqlParameterSource().addValue("item_code", itemCode),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    /**
     * 目录三维筛选列表(商家硬绑定;业态 = merchants.merchant_type 随商家带出,V348 五值;品类独立标签)。
     * includeDrafts/includeArchived 供目录页草案队列与归档档查看;默认只看生效行。
     * 读端敏感:price 是否可见由 controller 层权限闸决定(无权限根本进不来本查询)。
     */
    public List<CatalogEntryRow> listCatalogEntries(String merchantId, String businessFormat, String category,
                                                    boolean includeDrafts, boolean includeArchived) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.item_code, c.merchant_id, m.merchant_type AS business_format, c.category, c.item_name,
                       c.price_cent, c.status, c.is_draft, c.updated_by, c.updated_at
                  FROM internal_price_catalog c
                  LEFT JOIN merchants m ON m.id = c.merchant_id
                 WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (!includeArchived) {
            sql.append(" AND c.status = 'active'");
        }
        if (!includeDrafts) {
            sql.append(" AND c.is_draft = FALSE");
        }
        if (merchantId != null) {
            sql.append(" AND c.merchant_id = :merchant_id");
            params.addValue("merchant_id", merchantId);
        }
        if (businessFormat != null) {
            sql.append(" AND m.merchant_type = :business_format");
            params.addValue("business_format", businessFormat);
        }
        if (category != null) {
            sql.append(" AND c.category = :category");
            params.addValue("category", category);
        }
        sql.append(" ORDER BY c.is_draft DESC, c.updated_at DESC, c.item_code");
        return jdbcTemplate.query(sql.toString(), params, CATALOG_ROW_MAPPER);
    }

    /**
     * 目录转正后快照传播(V380 收编契约):四张业务表按引用码同步快照列 = 「改一次,对后续核销/结算生效」。
     * 只动快照列,不动任何流水级固化(ticket_verifications/voucher_redemptions/结算账单行) —— 已发生的账碰不到。
     * 与转正同事务(调用方 @Transactional):要么一起生效,要么一起回滚,不存在"目录已转正、快照还是旧价"的窗口。
     */
    public void propagateCatalogPriceToSnapshots(String itemCode, long priceCent) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("item_code", itemCode).addValue("price_cent", priceCent);
        jdbcTemplate.update("UPDATE bar_products SET internal_settlement_price_cent = :price_cent,"
                + " updated_at = now() WHERE catalog_item_code = :item_code", params);
        jdbcTemplate.update("UPDATE bar_combos SET internal_settlement_price_cent = :price_cent,"
                + " updated_at = now() WHERE catalog_item_code = :item_code", params);
        jdbcTemplate.update("UPDATE entitlement_templates SET agreement_price_cent = :price_cent,"
                + " updated_at = now() WHERE catalog_item_code = :item_code", params);
        jdbcTemplate.update("UPDATE voucher_definitions SET internal_price_cent = :price_cent,"
                + " updated_at = now() WHERE catalog_item_code = :item_code", params);
    }

    /** 草案行读取(id 定位;转正专案分支要拿 item_code/price 做 0 价守卫与快照传播)。 */
    public CatalogEntryRow findCatalogDraftById(String id) {
        List<CatalogEntryRow> rows = jdbcTemplate.query("""
                SELECT c.id, c.item_code, c.merchant_id, m.merchant_type AS business_format, c.category, c.item_name,
                       c.price_cent, c.status, c.is_draft, c.updated_by, c.updated_at
                  FROM internal_price_catalog c
                  LEFT JOIN merchants m ON m.id = c.merchant_id
                 WHERE c.id = :id AND c.is_draft = TRUE AND c.status = 'active'
                """, new MapSqlParameterSource().addValue("id", id), CATALOG_ROW_MAPPER);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 商户存在性(落草案先验:给明确 400 而不是 FK 违约 500)。 */
    public boolean merchantExists(String merchantId) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM merchants WHERE id = :id",
                new MapSqlParameterSource().addValue("id", merchantId), Integer.class);
        return n != null && n > 0;
    }

    /** FS-024:业态为「其他」且档案未指定主管公司(V511 supervising_company_id)的商户 → true(写入口据此拒绝开通结算/分账)。 */
    public boolean otherFormatWithoutSupervisingCompany(String merchantId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM merchants WHERE id = :id AND merchant_type = 'other' AND supervising_company_id IS NULL",
                new MapSqlParameterSource().addValue("id", merchantId), Integer.class);
        return n != null && n > 0;
    }

    /**
     * 目录条目转正 + 快照传播,一个事务(🔴 资金级原子性):Path 2 软替换/翻草案 与 四表快照同步要么全成、
     * 要么全回滚,不留"目录已新价、快照还旧价"的窗口。注意必须经 Spring 代理从服务层调进来(勿仓储内自调)。
     */
    @Transactional
    public int activateInternalPriceItemDraft(String id, String itemCode, long priceCent,
                                              String updatedBy, Instant now) {
        int rows = activateSingleRowDraft(SingleRowConfig.INTERNAL_PRICE_ITEM, id, updatedBy, now);
        if (rows > 0) {
            propagateCatalogPriceToSnapshots(itemCode, priceCent);
        }
        return rows;
    }

    /** 引用该条目的券定义数(转正 0 价守卫:券 internal_price 口径要求 >0,V301 chk_vd_internal_price)。 */
    public int countVoucherRefs(String itemCode) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM voucher_definitions WHERE catalog_item_code = :item_code",
                new MapSqlParameterSource().addValue("item_code", itemCode), Integer.class);
        return n == null ? 0 : n;
    }

    private static final org.springframework.jdbc.core.RowMapper<CatalogEntryRow> CATALOG_ROW_MAPPER = (rs, i) ->
            new CatalogEntryRow(rs.getString("id"), rs.getString("item_code"), rs.getString("merchant_id"),
                    rs.getString("business_format"), rs.getString("category"), rs.getString("item_name"),
                    rs.getLong("price_cent"), rs.getString("status"), rs.getBoolean("is_draft"),
                    rs.getString("updated_by"),
                    rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toInstant());

    /** 目录条目一行(读端投影;business_format 随商家带出)。 */
    public record CatalogEntryRow(String id, String itemCode, String merchantId, String businessFormat,
                                  String category, String itemName, long priceCent, String status, boolean isDraft,
                                  String updatedBy, java.time.Instant updatedAt) {
    }

    // ================= §3 复核转正(旧生效行软替换 status→inactive,不物理删)=================

    /**
     * 单行 scope 类型转正(Path 2 换行,@Transactional):
     * ① 旧生效行(is_draft=FALSE, status=active)软替换 status→inactive(留库);② 翻草案行为生效;③ 返回②行数(0=非草案/不存在)。
     * scope「同一」用 null 安全等价 IS NOT DISTINCT FROM(免 COALESCE 哨兵);表名+scope 列来自白名单枚举。
     */
    @Transactional
    public int activateSingleRowDraft(SingleRowConfig cfg, String id, String updatedBy, Instant now) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now));
        String activationSequenceUpdate = "";
        if (cfg == SingleRowConfig.TURNOVER_RENT_RULE) {
            // 同合同、商户的转正串行；锁后重读，重复批准不能停用已转正的规则。
            jdbcTemplate.query("""
                    SELECT pg_advisory_xact_lock(hashtextextended(
                        'turnover_rent_rule:' || jsonb_build_array(contract_id, merchant_id)::text, 0))
                      FROM turnover_rent_rules WHERE id = :id
                    """, params, rs -> { });
            List<LocalDate> draftEndDates = jdbcTemplate.query("""
                    SELECT effective_to FROM turnover_rent_rules
                     WHERE id = :id AND is_draft = TRUE AND status = 'active'
                     FOR UPDATE
                    """, params, (rs, rowNum) -> rs.getObject("effective_to", LocalDate.class));
            if (draftEndDates.isEmpty()) {
                return 0;
            }
            boolean replacement = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1 FROM turnover_rent_rules approved
                        JOIN turnover_rent_rules draft ON draft.id = :id
                         WHERE approved.contract_id IS NOT DISTINCT FROM draft.contract_id
                           AND approved.merchant_id IS NOT DISTINCT FROM draft.merchant_id
                           AND approved.is_draft = FALSE AND approved.id <> draft.id)
                    """, params, Boolean.class));
            if (replacement) {
                LocalDate effectiveFrom = now.atZone(LocalZone.ID).toLocalDate()
                        .withDayOfMonth(1).plusMonths(1);
                LocalDate effectiveTo = draftEndDates.get(0);
                if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
                    throw ApiException.badRequest("VALIDATION_FAILED",
                            "变更规则按批准次月1日（" + effectiveFrom + "）生效，结束日期不能早于该日期");
                }
                params.addValue("effective_from", effectiveFrom);
                activationSequenceUpdate = ", effective_from = :effective_from";
            }
            params.addValue("activation_sequence", jdbcTemplate.queryForObject(
                    "SELECT nextval('turnover_rent_rule_activation_seq')", params, Long.class));
            activationSequenceUpdate += ", activation_sequence = :activation_sequence";
        }
        StringBuilder scopeEq = new StringBuilder();
        for (String col : cfg.scopeColumns) {
            scopeEq.append(" AND l.").append(col).append(" IS NOT DISTINCT FROM d.").append(col);
        }
        // ① 旧生效行软替换 → inactive(仅 active 生效行;已 inactive 历史行不动)
        jdbcTemplate.update("UPDATE " + cfg.table + " l"
                + " SET status = 'inactive', updated_by = :updated_by, updated_at = :now"
                + " FROM " + cfg.table + " d"
                + " WHERE d.id = :id AND d.is_draft = TRUE AND d.status = 'active'"
                + " AND l.is_draft = FALSE AND l.status = 'active' AND l.id <> d.id" + scopeEq, params);
        // ② 翻草案为生效；抽成规则同时固化审批先后，之后替换和归档均不修改该序号。
        return jdbcTemplate.update("UPDATE " + cfg.table
                + " SET is_draft = FALSE, updated_by = :updated_by, updated_at = :now" + activationSequenceUpdate
                + " WHERE id = :id AND is_draft = TRUE AND status = 'active'", params);
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

    /** 单行 scope 表 active 行最后编辑人(V340 软删差人守卫用;表名取自白名单枚举,无注入面)。 */
    public String singleRowActiveUpdatedBy(SingleRowConfig cfg, String id) {
        return jdbcTemplate.query(
                "SELECT updated_by FROM " + cfg.table + " WHERE id = :id AND status = 'active'",
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

    /**
     * V340 单行 scope 表软删(仅用于 commission_rate / points_accrual_rate 的<b>商户级特例条目</b>):
     * status→inactive + archive_reason(留库不物理删),使该商户回落到项目/全局默认口径。表名取自白名单枚举。
     * 返回更新行数(0=不存在或已下线)。全局默认种子不该被软删——由服务层守卫拦(见 SettlementConfigWriteService.archive)。
     */
    public int softDeleteSingleRow(SingleRowConfig cfg, String id, String archiveReason,
                                   String updatedBy, Instant now) {
        return jdbcTemplate.update("UPDATE " + cfg.table
                + " SET status = 'inactive', archive_reason = :archive_reason,"
                + "     updated_by = :updated_by, updated_at = :now"
                + " WHERE id = :id AND status = 'active'",
                new MapSqlParameterSource()
                .addValue("id", id).addValue("archive_reason", archiveReason)
                .addValue("updated_by", updatedBy).addValue("now", java.sql.Timestamp.from(now)));
    }

    // ================= §5 待复核草案聚合(各配置表 is_draft=TRUE AND status='active' 的 UNION)=================

    /**
     * 全部结算配置表的待复核草案(供 overview 草案队列)。giveaway(废)/settlement_tax(零消费)不入。
     *
     * <p><b>本方法是有意的平台级全量读(无 project scope 过滤),不是隔离缺口</b>:唯一调用方
     * {@code SettlementConfigService.overview()} → {@code GET /api/v1/settlement/config/overview},
     * 该端点强制 {@code settlement.config.view} + {@code requirePlatformScope()}——平台管理员专属,
     * 项目级操作者在 controller 层就 403、到不了这里。「平台管理员跨项目审所有配置草案」正是本队列的语义。
     * 静态扫描器(verify:tenant-isolation)不识别端点级 scope 闸,同 revenue_stream_attribution 的
     * SEC-P2-TENANT-ISOLATION-BATCH1-PLATFORM-READ-001 台账口径;本表族的 scoped 读通道由
     * {@link JdbcCommissionPointsConfigRepository} / {@link JdbcSettlementBillRepository} 的 finder 提供。
     */
    public List<PendingConfigDraft> listPendingConfigDrafts() {
        return jdbcTemplate.query("""
                SELECT id, config_group, summary FROM (
                    SELECT id, 'distribution_method' AS config_group,
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 分配方式 ' || method_type
                               || ' 超池特批有效期 ' || special_approval_term_value
                               || CASE special_approval_term_unit WHEN 'day' THEN ' 天' ELSE ' 个月' END AS summary,
                           updated_at
                      FROM ticket_distribution_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'merchant_rate',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 商户=' || merchant_id
                               || CASE rule_type WHEN 'per_guest' THEN ' 第一段固定规则=每客单价 ' || per_guest_cent || ' 分'
                                                 ELSE ' 第一段固定规则=固定额 ' || fixed_amount_cent || ' 分' END,
                           updated_at
                      FROM ticket_distribution_merchant_rates WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    -- S2:票种维度。复核人要判断「这条费率影响哪个票」,故 join 出票种名而不是给一串 id。
                    SELECT c.id, 'comprehensive_service_fee',
                           'project=' || COALESCE(c.project_id, '__DEFAULT__') || ' 商户=' || COALESCE(c.merchant_id, '__ANY__')
                               || ' 票种=' || COALESCE(tt.ticket_name, '__ANY__')
                               || ' 费=' || CASE WHEN c.fixed_amount_cent IS NULL THEN c.fee_bps || 'bps' ELSE c.fixed_amount_cent || '分' END
                               || CASE WHEN c.monthly_cap_amount_cent IS NULL THEN '' ELSE ' 封顶' || c.monthly_cap_amount_cent || '分' END,
                           c.updated_at
                      FROM comprehensive_service_fee_configs c
                      LEFT JOIN ticket_types tt ON tt.id = c.ticket_type_id
                     WHERE c.is_draft = TRUE AND c.status = 'active'
                    UNION ALL
                    SELECT id, 'combo_discount',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 折扣归属 ' || discount_bearer
                               || ' 凑整允许误差 ' || rounding_tolerance_cent || '分',
                           updated_at
                      FROM ticket_combo_discount_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'channel_fee',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 渠道=' || COALESCE(channel, '__ALL__')
                               || CASE WHEN fixed_amount_cent IS NOT NULL
                                       THEN ' 每张票固定 ' || fixed_amount_cent || '分'
                                       ELSE ' 抽成 ' || fee_bps || 'bps' END,
                           updated_at
                      FROM channel_fee_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'commission_rate',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 商户=' || COALESCE(merchant_id, '__ANY__')
                               || ' 角色=' || role_type || ' 提成 ' || rate_bp || 'bps',
                           updated_at
                      FROM bar_commission_rate_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'points_accrual_rate',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 商户=' || COALESCE(merchant_id, '__ANY__')
                               || ' 每积1分需消费 ' || amount_cent_per_point || '分',
                           updated_at
                      FROM points_accrual_rate_configs WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'high_value_threshold',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 商户=' || COALESCE(merchant_id, '__ANY__')
                               || ' 高价值判定阈值(进货价) ' || high_value_threshold_cent || '分',
                           updated_at
                      FROM high_value_thresholds WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT MIN(id) AS id, 'rank_tiers',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' 排名档位草案(' || COUNT(*) || ' 档)',
                           MAX(updated_at) AS updated_at
                      FROM ticket_distribution_rank_tiers WHERE is_draft = TRUE AND status = 'active'
                      GROUP BY project_id
                    UNION ALL
                    -- V355:per_merchant 归属主体列为空,直接 || NULL 会把整条 summary 变成 NULL(草案队列显示空白)。
                    SELECT id, 'revenue_stream_attribution',
                           'project=' || COALESCE(project_id, '__DEFAULT__') || ' ' || revenue_stream || ' → '
                           || CASE WHEN beneficiary_kind = 'per_merchant' THEN '各商户自己'
                                   ELSE COALESCE(management_entity_id, '(未指定主体)') END,
                           updated_at
                      FROM revenue_stream_attribution WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    SELECT id, 'attribution_rule',
                           rule_scope || ' / ' || revenue_source || ' · ' || attribution_mode || ' · ' || share_bps || 'bps',
                           updated_at
                      FROM merchant_revenue_attribution_rules WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    -- S6 赠送权益预扣规则草案:项目 × 票种。复核人要看得出两模式与金额,join 出票名。
                    SELECT g.id, 'pass_gift_cost',
                           'project=' || COALESCE(g.project_id, '__DEFAULT__')
                               || ' 票种=' || COALESCE(tt.ticket_name, '__ANY__')
                               || ' 模式=' || CASE g.prededuct_mode WHEN 'by_item' THEN '按件计' ELSE '按票计' END
                               || COALESCE(' 整额' || g.ticket_gift_amount_cent || '分', '')
                               || CASE WHEN g.monthly_cap_amount_cent IS NULL THEN ''
                                       ELSE ' 封顶' || g.monthly_cap_amount_cent || '分' END,
                           g.updated_at
                      FROM pass_gift_cost_configs g
                      LEFT JOIN ticket_types tt ON tt.id = g.ticket_type_id
                     WHERE g.is_draft = TRUE AND g.status = 'active'
                    UNION ALL
                    -- V346（P0-E）营业额抽成规则草案：合同 × 商户，档位明细挂 turnover_rent_rule_tiers（随规则行整体转正）。
                    SELECT id, 'turnover_rent_rule',
                           '合同=' || COALESCE(contract_id, '-') || ' 商户=' || merchant_id
                               || ' 规则=' || rule_name
                               || ' 保底=' || COALESCE(min_amount_cent::text, '-') || '分'
                               || ' 生效起=' || effective_from,
                           updated_at
                      FROM turnover_rent_rules WHERE is_draft = TRUE AND status = 'active'
                    UNION ALL
                    -- 批10/E 内部结算 SKU 目录条目草案:复核人要看清「哪家商户的哪个 SKU 改成多少钱」。
                    SELECT c.id, 'internal_price_item',
                           '商户=' || COALESCE(m.merchant_name, c.merchant_id) || ' 品类=' || c.category
                               || ' 条目=' || c.item_name || ' 内部结算价 ' || c.price_cent || '分',
                           c.updated_at
                      FROM internal_price_catalog c
                      LEFT JOIN merchants m ON m.id = c.merchant_id
                     WHERE c.is_draft = TRUE AND c.status = 'active'
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
