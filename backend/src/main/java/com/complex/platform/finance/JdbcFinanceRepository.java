package com.complex.platform.finance;

// 文件版本：V0.24
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcFinanceRepository implements FinanceRepository {

    private static final String AR_AP_ITEM_SELECT = """
            SELECT id, item_no, direction, counterparty_id, source_type, source_id,
                   amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                   accounting_period, status, created_at, updated_at, merchant_id, project_id,
                   origin_item_id, adjustment_id
              FROM ar_ap_items
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    // 第十八批 Q 段第 138 条 138-R1：BarRevenueSourceService→BarPayrollAllocationService 依赖 FinanceRepository，直接注入会成环，按需取。
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<com.complex.platform.bar.BarRevenueSourceService> barRevenueSources;

    JdbcFinanceRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper.copy().findAndRegisterModules();
    }

    @Override
    public ArApItemRecord saveArApItem(ArApItemRecord item) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (
                    id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                    accounting_period, status, created_at, updated_at, merchant_id, project_id,
                    origin_item_id, adjustment_id
                )
                VALUES (
                    :id, :item_no, :direction, :counterparty_id, :source_type, :source_id,
                    :amount_cent, :paid_amount_cent, :unpaid_amount_cent, :currency, :due_date,
                    :accounting_period, :status, :created_at, :updated_at, :merchant_id, :project_id,
                    :origin_item_id, :adjustment_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    item_no = EXCLUDED.item_no,
                    direction = EXCLUDED.direction,
                    counterparty_id = EXCLUDED.counterparty_id,
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    amount_cent = EXCLUDED.amount_cent,
                    paid_amount_cent = EXCLUDED.paid_amount_cent,
                    unpaid_amount_cent = EXCLUDED.unpaid_amount_cent,
                    currency = EXCLUDED.currency,
                    due_date = EXCLUDED.due_date,
                    accounting_period = EXCLUDED.accounting_period,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, arApItemParameters(item));
        return item;
    }

    @Override
    public ArApItemRecord appendArApItem(ArApItemRecord item) {
        // P0-B（V344）红冲专用纯追加写入：与 saveArApItem 的 upsert 不同，命中既有 id 一律 DO NOTHING，
        // 保证冲正行/重述行一经落表就再也不会被同 id 重放改写（append-only 语义在写入侧也不留口子）。
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (
                    id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                    accounting_period, status, created_at, updated_at, merchant_id, project_id,
                    origin_item_id, adjustment_id
                )
                VALUES (
                    :id, :item_no, :direction, :counterparty_id, :source_type, :source_id,
                    :amount_cent, :paid_amount_cent, :unpaid_amount_cent, :currency, :due_date,
                    :accounting_period, :status, :created_at, :updated_at, :merchant_id, :project_id,
                    :origin_item_id, :adjustment_id
                )
                ON CONFLICT (id) DO NOTHING
                """, arApItemParameters(item));
        return item;
    }

    @Override
    public ArApItemSourceRecord saveArApItemSource(ArApItemSourceRecord source) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_item_sources (
                    id, ar_ap_item_id, source_type, source_id, amount_cent, created_at
                )
                VALUES (
                    :id, :ar_ap_item_id, :source_type, :source_id, :amount_cent, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id,
                    source_type = EXCLUDED.source_type,
                    source_id = EXCLUDED.source_id,
                    amount_cent = EXCLUDED.amount_cent
                """, arApItemSourceParameters(source));
        return source;
    }

    @Override
    public List<ArApItemRecord> listArApItems() {
        return jdbcTemplate.query(AR_AP_ITEM_SELECT + """
                 ORDER BY created_at DESC, id
                """, this::mapArApItem);
    }

    @Override
    public List<ArApItemRecord> listArApItemsByMerchant(String merchantId) {
        return jdbcTemplate.query(AR_AP_ITEM_SELECT + """
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), this::mapArApItem);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ArApItemRecord> listArApItemsPaged(
            String merchantId, String keyword, int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 200);
        String trimmed = keyword == null ? "" : keyword.trim();
        // 租户隔离谓词与 listArApItems/ByMerchant 完全同口径:merchantId 非空才加 merchant_id 过滤(平台口径不加)。
        // 关键词跨 单号/方向/状态 服务端过滤,ILIKE 大小写不敏感;参数化绑定,无拼接注入。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("kw", trimmed.isEmpty() ? null : "%" + trimmed + "%", java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (:kw IS NULL OR item_no ILIKE :kw OR direction ILIKE :kw OR status ILIKE :kw OR source_type ILIKE :kw)
                """;
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ar_ap_items" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<ArApItemRecord> items = jdbcTemplate.query(AR_AP_ITEM_SELECT + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapArApItem);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ArApItemRecord> listArApItemsPaged(
            String merchantId, String projectId, String keyword, int page, int size) {
        return listArApItemsForProjects(merchantId,projectId==null ? null : java.util.Set.of(projectId),keyword,page,size);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ArApItemRecord> listArApItemsForProjects(
            String merchantId, java.util.Set<String> projectIds, String keyword, int page, int size) {
        return listArApItemsForProjects(merchantId,projectIds,keyword,page,size,null,null);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<ArApItemRecord> listArApItemsForProjects(
            String merchantId, java.util.Set<String> projectIds, String keyword, int page, int size,
            java.util.Set<String> payrollItemIds, String lockedDayCloseId) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 200);
        String trimmed = keyword == null ? "" : keyword.trim();
        if (projectIds != null && projectIds.isEmpty())
            return new com.complex.platform.common.api.PageResponse<>(List.of(),safePage,safeSize,0L);
        // 同一个筛选谓词用于行和总数，分页之前取商户与授权项目交集。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_ids", projectIds)
                .addValue("kw", trimmed.isEmpty() ? null : "%" + trimmed + "%", java.sql.Types.VARCHAR)
                .addValue("locked_day_close_id", lockedDayCloseId);
        String where = """
                 WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (:kw IS NULL OR item_no ILIKE :kw OR direction ILIKE :kw OR status ILIKE :kw OR source_type ILIKE :kw)
                """ + (projectIds == null ? "" : " AND project_id IN (:project_ids)")
                // 第十八批 R 段 143-R4：按锁定该台账的日结过滤（日结页面「本日结锁定的台账」）。
                + (lockedDayCloseId == null ? "" : " AND locked_day_close_id = :locked_day_close_id");
        if (payrollItemIds != null) {
            if (payrollItemIds.isEmpty()) where += " AND source_type <> 'staff_payroll'";
            else {
                params.addValue("payroll_ids", payrollItemIds);
                where += " AND (source_type <> 'staff_payroll' OR id IN (:payroll_ids))";
            }
        }
        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ar_ap_items" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<ArApItemRecord> items = jdbcTemplate.query(AR_AP_ITEM_SELECT + where + """
                 ORDER BY created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapArApItem);
        return new com.complex.platform.common.api.PageResponse<>(
                items, safePage, safeSize, total == null ? 0L : total);
    }

    @Override
    public Optional<ArApItemRecord> findArApItemById(String id) {
        return jdbcTemplate.query(AR_AP_ITEM_SELECT + """
                 WHERE id = :id
                """, Map.of("id", id), this::mapArApItem).stream().findFirst();
    }

    @Override
    public Optional<ArApItemRecord> findArApItemByIdForUpdate(String id) {
        return jdbcTemplate.query(AR_AP_ITEM_SELECT + """
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), this::mapArApItem).stream().findFirst();
    }

    @Override
    public boolean hasCorrectionItems(String itemId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ar_ap_items WHERE origin_item_id = :id)", Map.of("id", itemId), Boolean.class));
    }

    @Override
    public boolean isPurchasePayableReplaced(String itemId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM purchase_payable_adjustments WHERE original_item_id = :id)
                    OR EXISTS (SELECT 1 FROM purchase_prepayment_resolutions WHERE original_item_id = :id AND status = 'posted')
                    OR EXISTS (SELECT 1 FROM ar_ap_adjustments WHERE ar_ap_item_id = :id AND approval_status = 'approved')
                """, Map.of("id", itemId), Boolean.class));
    }

    @Override
    public Optional<ArApItemRecord> findCurrentPurchasePayable(String sourceType, String sourceId) {
        List<ArApItemRecord> current = jdbcTemplate.query(AR_AP_ITEM_SELECT + """
                 WHERE direction = 'payable' AND source_type = :source_type AND source_id = :source_id
                   AND amount_cent > 0 AND status <> 'reversed'
                   AND NOT EXISTS (SELECT 1 FROM purchase_payable_adjustments p WHERE p.original_item_id = ar_ap_items.id)
                   AND NOT EXISTS (SELECT 1 FROM purchase_prepayment_resolutions p
                                    WHERE p.original_item_id = ar_ap_items.id AND p.status = 'posted')
                   AND NOT EXISTS (SELECT 1 FROM ar_ap_adjustments a
                                    WHERE a.ar_ap_item_id = ar_ap_items.id AND a.approval_status = 'approved')
                """, Map.of("source_type", sourceType, "source_id", sourceId), this::mapArApItem);
        if (current.size() > 1) throw ApiException.conflict(
                "PURCHASE_PAYABLE_AMBIGUOUS", "采购来源存在多张有效应付，请核实更正链");
        return current.stream().findFirst();
    }

    @Override
    public ArApItemRecord updateArApItemLedger(ArApItemRecord item) {
        jdbcTemplate.update("""
                UPDATE ar_ap_items
                   SET paid_amount_cent = :paid_amount_cent,
                       unpaid_amount_cent = :unpaid_amount_cent,
                       status = :status,
                       updated_at = :updated_at
                 WHERE id = :id
                """, arApItemParameters(item));
        return item;
    }

    @Override
    public boolean existsArApItemBySource(String direction, String sourceType, String sourceId) {
        // S6：对齐 uq_ar_ap_items_direction_source 幂等键维度的存在性判断（冲销从未挂出的应付须 no-op）。
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM ar_ap_items
                                WHERE direction = :direction AND source_type = :source_type AND source_id = :source_id)
                """, new MapSqlParameterSource()
                .addValue("direction", direction)
                .addValue("source_type", sourceType)
                .addValue("source_id", sourceId), Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<ArApItemRecord> findReserveWithheldByStatementForUpdate(String statementId) {
        // V304：某计算单的待释放留存应付（唯一：uq(direction,source_type,source_id)），FOR UPDATE 行锁串行化并发释放。
        return jdbcTemplate.query(AR_AP_ITEM_SELECT + """
                 WHERE direction = 'payable'
                   AND source_type = 'dividend_reserve_withheld'
                   AND source_id = :statement_id
                   FOR UPDATE
                """, Map.of("statement_id", statementId), this::mapArApItem).stream().findFirst();
    }

    @Override
    public int retagArApItemSourceType(String id, String expectedSourceType, String newSourceType, Instant now) {
        // V304：re-tag 源类型（前态守卫幂等——仅当现值=expected 才翻）；append-only(V145 delete_only) 允许 UPDATE。
        return jdbcTemplate.update("""
                UPDATE ar_ap_items
                   SET source_type = :new_source_type, updated_at = :now
                 WHERE id = :id AND source_type = :expected_source_type
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("expected_source_type", expectedSourceType)
                .addValue("new_source_type", newSourceType)
                .addValue("now", timestampWithTimeZone(now)));
    }

    @Override
    public ArApPaymentRecord saveArApPayment(ArApPaymentRecord payment) {
        // DO NOTHING on conflict: idempotency is enforced at the service layer via IdempotencyService.
        // The payment ID includes the ar_ap_item_id in its slug (fixed P0 collision), so a genuine
        // duplicate ID can only arise from an exact idempotent retry that is already handled upstream.
        jdbcTemplate.update("""
                INSERT INTO ar_ap_payments (
                    id, ar_ap_item_id, amount_cent, payment_method, payment_time,
                    bank_flow_ref, created_at
                )
                VALUES (
                    :id, :ar_ap_item_id, :amount_cent, :payment_method, :payment_time,
                    :bank_flow_ref, :created_at
                )
                ON CONFLICT (id) DO NOTHING
                """, arApPaymentParameters(payment));
        return payment;
    }

    @Override
    public Optional<ArApPaymentRecord> findArApPaymentById(String id) {
        return jdbcTemplate.query("""
                SELECT id, ar_ap_item_id, amount_cent, payment_method, payment_time,
                       bank_flow_ref, created_at
                  FROM ar_ap_payments
                 WHERE id = :id
                """, Map.of("id", id), this::mapArApPayment).stream().findFirst();
    }

    @Override
    public Optional<ArApPaymentRecord> findArApPaymentByIdForUpdate(String id) {
        // P1 修复：退款前对 target payment 行加 FOR UPDATE 行锁，使并发退款在 sumRefundedAmount 累计校验前串行化，
        // 杜绝 TOCTOU 导致退款合计超过 payment 金额。
        return jdbcTemplate.query("""
                SELECT id, ar_ap_item_id, amount_cent, payment_method, payment_time,
                       bank_flow_ref, created_at
                  FROM ar_ap_payments
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), this::mapArApPayment).stream().findFirst();
    }

    @Override
    public long sumRefundedAmountByTargetId(String targetId) {
        Long result = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM finance_refunds
                 WHERE target_id = :target_id AND source_type = 'ar_ap_payment'
                """, Map.of("target_id", targetId), Long.class);
        return result == null ? 0L : result;
    }

    @Override
    public long sumSucceededRefundAmountByTargetId(String targetId) {
        Long amount = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(refund_amount_cent), 0) FROM finance_refunds "
                + "WHERE target_id = :target AND source_type = 'ar_ap_payment' AND status = 'success'",
                Map.of("target", targetId), Long.class);
        return amount == null ? 0 : amount;
    }

    @Override
    public DailyCloseRecord saveDailyClose(DailyCloseRecord dailyClose) {
        jdbcTemplate.update("""
                INSERT INTO finance_daily_closes (
                    id, business_date, scope_type, scope_id, status, locked_at, created_at, confirmed_at, archived_at
                )
                VALUES (
                    :id, :business_date, :scope_type, :scope_id, :status, :locked_at, :created_at,
                    :confirmed_at, :archived_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_date = EXCLUDED.business_date,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    status = EXCLUDED.status,
                    locked_at = EXCLUDED.locked_at,
                    confirmed_at = EXCLUDED.confirmed_at,
                    archived_at = EXCLUDED.archived_at
                """, dailyCloseParameters(dailyClose));
        return dailyClose;
    }

    @Override
    public boolean openTicketVerificationAnomalyExists(Instant from, Instant to, String scopeType, String scopeId) {
        boolean merchantScope = isMerchantScope(scopeType);
        // 第十八批 S 段第 161 条 161-R2：project 范围只看原核销所属订单属该综合体、核销商户不是酒吧的（单店酒吧门票随酒吧日结）。
        boolean projectScope = "project".equals(scopeType);
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM ticket_verifications a
                      JOIN ticket_verifications o ON o.id = a.anomaly_of_verification_id
                     WHERE a.status = 'anomaly'
                       AND a.anomaly_resolution IS NULL
                       AND o.created_at >= :from
                       AND o.created_at < :to
                       AND (:merchant_scope = FALSE OR o.merchant_id = :merchant_id)
                       AND (:project_scope = FALSE
                            OR (EXISTS (SELECT 1 FROM issued_entitlements e JOIN ticket_orders t ON t.id = e.order_id
                                         WHERE e.id = o.entitlement_id AND t.project_id = :project_id)
                                AND NOT EXISTS (SELECT 1 FROM merchants m WHERE m.id = o.merchant_id AND m.merchant_type = 'bar'))))
                """, new MapSqlParameterSource()
                .addValue("from", from.atOffset(java.time.ZoneOffset.UTC))
                .addValue("to", to.atOffset(java.time.ZoneOffset.UTC))
                .addValue("merchant_scope", merchantScope)
                .addValue("merchant_id", merchantScope ? scopeId : null)
                .addValue("project_scope", projectScope)
                .addValue("project_id", projectScope ? scopeId : null), Boolean.class));
    }

    @Override
    public boolean openBarBillAnomalyExists(Instant from, Instant to, String scopeType, String scopeId) {
        return openBarBillAnomalyExists(from, to, scopeType, scopeId, Map.of());
    }

    @Override
    public boolean openBarBillAnomalyExists(Instant from, Instant to, String scopeType, String scopeId,
            Map<String, Instant[]> barWindows) {
        if ("project".equals(scopeType)) {
            return false;  // 第十八批 S 段第 161 条 161-R2：酒吧单据只属酒吧日结（酒吧逐店另出），综合体日结不看
        }
        boolean merchantScope = isMerchantScope(scopeType);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from.atOffset(java.time.ZoneOffset.UTC))
                .addValue("to", to.atOffset(java.time.ZoneOffset.UTC))
                .addValue("merchant_scope", merchantScope)
                .addValue("merchant_id", merchantScope ? scopeId : null);
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM bar_bills b
                     WHERE b.payment_status = 'abnormal'
                       AND (:merchant_scope = FALSE OR b.merchant_id = :merchant_id)
                       AND (%s
                            OR EXISTS (SELECT 1 FROM bar_bill_settlements s
                                        WHERE s.bill_id = b.id AND %s)))
                """.formatted(barWindowPredicate("b.created_at", "b.bar_id", barWindows, params),
                barWindowPredicate("s.settled_at", "b.bar_id", barWindows, params)), params, Boolean.class));
    }

    @Override
    public List<String> unsettledBarBillNos(Instant from, Instant to, String scopeType, String scopeId,
            Map<String, Instant[]> barWindows) {
        if ("project".equals(scopeType)) {
            return List.of();  // 161-R2：同酒吧账单异常，综合体日结不看
        }
        boolean merchantScope = isMerchantScope(scopeType);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from.atOffset(java.time.ZoneOffset.UTC))
                .addValue("to", to.atOffset(java.time.ZoneOffset.UTC))
                .addValue("merchant_scope", merchantScope)
                .addValue("merchant_id", merchantScope ? scopeId : null);
        // 第十八批 T 段第 157 条 157-R3（BSM:1109「经理确认最终无法收款后转为坏账损耗处理中并解除该订单对交班、日结的阻断」）：
        // 所属订单（或合单后改指本账单）挂起已转坏账、已核销结案的不算未结清。
        return jdbcTemplate.queryForList("""
                SELECT b.bill_no
                  FROM bar_bills b
                 WHERE b.settlement_status NOT IN ('settled', 'merged', 'void')
                   AND (:merchant_scope = FALSE OR b.merchant_id = :merchant_id)
                   AND NOT EXISTS (SELECT 1 FROM bar_order_suspensions os WHERE (os.order_id = b.order_id OR os.bill_id = b.id)
                                      AND os.status IN ('bad_debt_processing', 'written_off_closed'))
                   AND %s
                 ORDER BY b.created_at, b.bill_no
                 LIMIT 10
                """.formatted(barWindowPredicate("b.created_at", "b.bar_id", barWindows, params)), params, String.class);
    }

    @Override
    public boolean unfinishedBarShiftExists(Instant from, Instant to, String scopeType, String scopeId,
            Map<String, Instant[]> barWindows) {
        return barShiftExists("s.witnessed_at IS NULL", from, to, scopeType, scopeId, barWindows);
    }

    /** 第十八批 R 段 143-R6：有差异待复核＝已班结且 needs_review（142-R1，V595 文件头）。 */
    @Override
    public boolean unbalancedBarShiftExists(Instant from, Instant to, String scopeType, String scopeId,
            Map<String, Instant[]> barWindows) {
        return barShiftExists("s.status = 'closed' AND s.needs_review = TRUE", from, to, scopeType, scopeId, barWindows);
    }

    /** 第十八批 S 段第 158 条 158-R1：未关闭的高价值对账差异（财务差异＋第三方差异），口径见接口。 */
    @Override
    public boolean openHighValueReconciliationDifferenceExists(java.time.LocalDate businessDate, String scopeType, String scopeId,
            Long thresholdCent) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT EXISTS (" + openDifferenceAmounts(
                        "b.business_date = :business_date AND (:threshold IS NULL OR d.amount_cent > :threshold)",
                        ":business_date BETWEEN b.period_start AND b.period_end"
                                + " AND (:threshold IS NULL OR d.difference_amount_cent > :threshold)") + ")",
                openDifferenceParams(businessDate, scopeType, scopeId, thresholdCent), Boolean.class));
    }

    /** 第十八批 Y 段第 182 条 182-R1：营业日不晚于本日、未关闭的低价值对账差异的笔数与金额合计，口径见接口。 */
    @Override
    public DailyCloseSourceTotals lowValueOpenDifferenceTotals(java.time.LocalDate businessDate, String scopeType, String scopeId,
            Long thresholdCent) {
        if (thresholdCent == null) {
            return DailyCloseSourceTotals.empty();  // ENUM:191「阈值未配置一律按高价值处理」，由日结闸拦
        }
        return jdbcTemplate.queryForObject("SELECT COUNT(*) AS order_count, COALESCE(SUM(amount_cent), 0) AS amount_cent FROM ("
                        + openDifferenceAmounts("b.business_date <= :business_date AND d.amount_cent <= :threshold",
                                "b.period_start <= :business_date AND d.difference_amount_cent <= :threshold") + ") x",
                openDifferenceParams(businessDate, scopeType, scopeId, thresholdCent),
                (rs, rowNumber) -> new DailyCloseSourceTotals(rs.getLong("amount_cent"), 0L, rs.getInt("order_count")));
    }

    /**
     * 158-R1、182-R1 共用：按日结范围取未关闭对账差异的金额行（列 amount_cent）——财务差异 status 不是 closed，第三方差异 status 不是 resolved、matched；
     * financeCondition、thirdPartyCondition 分别是财务、第三方差异的营业日与金额条件。merchant、bar 范围只看本商户（财务批次范围为该商户、
     * 第三方差异的回流门店为该商户）；第十八批 S 段第 161 条 161-R2：project 范围＝该综合体除酒吧外的业务——财务批次范围为该综合体或其非酒吧商户，
     * 第三方差异的回流门店属该综合体且不是酒吧；其余平台口径。
     */
    private static String openDifferenceAmounts(String financeCondition, String thirdPartyCondition) {
        return """
                SELECT d.amount_cent AS amount_cent
                  FROM finance_reconciliation_differences d
                  JOIN finance_reconciliation_batches b ON b.id = d.batch_id
                 WHERE d.status <> 'closed'
                   AND %s
                   AND (:merchant_scope = FALSE OR (b.scope_type IN ('merchant', 'bar') AND b.scope_id = :merchant_id))
                   AND (:project_scope = FALSE OR (b.scope_type = 'project' AND b.scope_id = :project_id)
                        OR (b.scope_type = 'merchant' AND EXISTS (SELECT 1 FROM merchants m WHERE m.id = b.scope_id
                            AND m.project_id = :project_id AND m.merchant_type <> 'bar')))
                UNION ALL
                SELECT d.difference_amount_cent
                  FROM integration_reconciliation_differences d
                  JOIN integration_reconciliation_batches b ON b.id = d.batch_id
                 WHERE d.status NOT IN ('resolved', 'matched')
                   AND %s
                   AND (:merchant_scope = FALSE OR EXISTS (
                        SELECT 1
                          FROM integration_order_syncs s
                         WHERE s.provider_code = d.provider_code
                           AND s.business_type = d.business_type
                           AND s.external_business_id = d.external_business_id
                           AND s.merchant_id = :merchant_id))
                   AND (:project_scope = FALSE OR EXISTS (
                        SELECT 1
                          FROM integration_order_syncs s
                          JOIN merchants m ON m.id = s.merchant_id
                         WHERE s.provider_code = d.provider_code
                           AND s.business_type = d.business_type
                           AND s.external_business_id = d.external_business_id
                           AND m.project_id = :project_id
                           AND m.merchant_type <> 'bar'))
                """.formatted(financeCondition, thirdPartyCondition);
    }

    private MapSqlParameterSource openDifferenceParams(java.time.LocalDate businessDate, String scopeType, String scopeId,
            Long thresholdCent) {
        boolean merchantScope = isMerchantScope(scopeType);
        boolean projectScope = "project".equals(scopeType);
        return new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("threshold", thresholdCent, java.sql.Types.BIGINT)
                .addValue("merchant_scope", merchantScope)
                .addValue("merchant_id", merchantScope ? scopeId : null, java.sql.Types.VARCHAR)
                .addValue("project_scope", projectScope)
                .addValue("project_id", projectScope ? scopeId : null, java.sql.Types.VARCHAR);
    }

    /** 开班落在该营业日（酒吧逐家按各自营业日窗）且满足 condition 的收银班次；merchant、bar 范围只看本酒吧，project 范围不看，其余平台口径。 */
    private boolean barShiftExists(String condition, Instant from, Instant to, String scopeType, String scopeId,
            Map<String, Instant[]> barWindows) {
        if ("project".equals(scopeType)) {
            return false;  // 第十八批 S 段第 161 条 161-R2：酒吧单据只属酒吧日结（酒吧逐店另出），综合体日结不看
        }
        boolean merchantScope = isMerchantScope(scopeType);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from.atOffset(java.time.ZoneOffset.UTC))
                .addValue("to", to.atOffset(java.time.ZoneOffset.UTC))
                .addValue("merchant_scope", merchantScope)
                .addValue("merchant_id", merchantScope ? scopeId : null);
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                      FROM bar_shifts s
                     WHERE %s
                       AND (:merchant_scope = FALSE OR s.bar_id = :merchant_id)
                       AND %s)
                """.formatted(condition, barWindowPredicate("s.opened_at", "s.bar_id", barWindows, params)), params, Boolean.class));
    }

    /**
     * 第十八批第 1 条（ENUM:187「按每个项目、每家酒吧可配」）：时刻列按所属酒吧各自的营业日窗判定；
     * windows 之外的酒吧（含没有主数据的历史 bar_id）用 [:from, :to)。
     */
    private static String barWindowPredicate(String column, String barIdSql, Map<String, Instant[]> windows,
            MapSqlParameterSource params) {
        String fallback = column + " >= :from AND " + column + " < :to";
        if (windows.isEmpty()) {
            return "(" + fallback + ")";
        }
        StringBuilder sql = new StringBuilder("(CASE ").append(barIdSql);
        int i = 0;
        for (Map.Entry<String, Instant[]> window : windows.entrySet()) {
            String key = "bw" + i++;
            params.addValue(key, window.getKey())
                    .addValue(key + "_from", timestampWithTimeZone(window.getValue()[0]))
                    .addValue(key + "_to", timestampWithTimeZone(window.getValue()[1]));
            sql.append(" WHEN :").append(key).append(" THEN ").append(column).append(" >= :").append(key)
                    .append("_from AND ").append(column).append(" < :").append(key).append("_to");
        }
        return sql.append(" ELSE ").append(fallback).append(" END)").toString();
    }

    @Override
    public boolean dailyCloseExists(LocalDate businessDate, String scopeType, String scopeId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM finance_daily_closes
                 WHERE business_date = :business_date
                   AND scope_type = :scope_type
                   AND scope_id = :scope_id
                """, new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("scope_type", scopeType)
                .addValue("scope_id", scopeId),
                Integer.class);
        return count != null && count > 0;
    }

    @Override
    public PaymentIntentRecord savePaymentIntent(PaymentIntentRecord paymentIntent) {
        jdbcTemplate.update("""
                INSERT INTO finance_payment_intents (
                    id, business_order_type, business_order_id, amount_cent, currency,
                    channel, status, gateway_provider, gateway_ref, gateway_status,
                    created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency,
                    :channel, :status, :gateway_provider, :gateway_ref, :gateway_status,
                    :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_order_type = EXCLUDED.business_order_type,
                    business_order_id = EXCLUDED.business_order_id,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    gateway_provider = EXCLUDED.gateway_provider,
                    gateway_ref = EXCLUDED.gateway_ref,
                    gateway_status = EXCLUDED.gateway_status,
                    updated_at = EXCLUDED.updated_at
                """, paymentIntentParameters(paymentIntent));
        return paymentIntent;
    }

    @Override
    public Optional<PaymentIntentRecord> findPaymentIntentById(String id) {
        return jdbcTemplate.query("""
                SELECT id, business_order_type, business_order_id, amount_cent, currency,
                       channel, status, gateway_provider, gateway_ref, gateway_status,
                       created_at, updated_at
                  FROM finance_payment_intents
                 WHERE id = :id
                """, Map.of("id", id), this::mapPaymentIntent).stream().findFirst();
    }

    @Override
    public Optional<PaymentIntentRecord> findPaymentIntentByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, business_order_type, business_order_id, amount_cent, currency,
                       channel, status, gateway_provider, gateway_ref, gateway_status, created_at, updated_at
                  FROM finance_payment_intents WHERE id = :id FOR UPDATE
                """, Map.of("id", id), this::mapPaymentIntent).stream().findFirst();
    }

    private PaymentIntentRecord mapPaymentIntent(ResultSet rs, int rowNumber) throws SQLException {
        return new PaymentIntentRecord(
                rs.getString("id"),
                rs.getString("business_order_type"),
                rs.getString("business_order_id"),
                rs.getLong("amount_cent"),
                rs.getString("currency"),
                rs.getString("channel"),
                rs.getString("status"),
                rs.getString("gateway_provider"),
                rs.getString("gateway_ref"),
                rs.getString("gateway_status"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    @Override
    public OfflinePaymentConfirmationRecord saveOfflinePaymentConfirmation(OfflinePaymentConfirmationRecord confirmation) {
        jdbcTemplate.update("""
                INSERT INTO finance_offline_payment_confirmations (
                    id, payment_id, amount_cent, payment_method, confirmed_at,
                    evidence_attachment_ids, status, created_at
                )
                VALUES (
                    :id, :payment_id, :amount_cent, :payment_method, :confirmed_at,
                    CAST(:evidence_attachment_ids AS jsonb), :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    payment_id = EXCLUDED.payment_id,
                    amount_cent = EXCLUDED.amount_cent,
                    payment_method = EXCLUDED.payment_method,
                    confirmed_at = EXCLUDED.confirmed_at,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    status = EXCLUDED.status
                """, offlineConfirmationParameters(confirmation));
        return confirmation;
    }

    @Override
    public void lockRefundSource(String refundId) {
        jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(:id, 0))",
                Map.of("id", refundId), Object.class);
    }

    @Override
    public FinanceRefundRecord saveRefund(FinanceRefundRecord refund) {
        jdbcTemplate.update("""
                INSERT INTO finance_refunds (
                    id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref,
                    gateway_provider, gateway_ref, updated_at, succeeded_at
                )
                VALUES (
                    :id, :target_id, :refund_amount_cent, :reason, :refund_channel, :refund_direction, :status, :created_at,
                    :source_type, :merchant_id, :project_id, :requested_by, :original_payment_ref,
                    :gateway_provider, :gateway_ref, :updated_at, :succeeded_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    gateway_provider = EXCLUDED.gateway_provider,
                    gateway_ref = EXCLUDED.gateway_ref,
                    updated_at = EXCLUDED.updated_at,
                    succeeded_at = EXCLUDED.succeeded_at
                """, refundParameters(refund));
        return refund;
    }

    @Override
    public List<String> listMerchantOperatingProjectIds(String merchantId) {
        return jdbcTemplate.queryForList("""
                SELECT project_id FROM merchants WHERE id = :merchant AND project_id IS NOT NULL
                UNION
                SELECT project_id FROM merchant_projects WHERE merchant_id = :merchant
                """, Map.of("merchant", merchantId), String.class);
    }

    @Override
    public Optional<FinanceRefundRecord> findRefundById(String id, boolean forUpdate) {
        return jdbcTemplate.query("SELECT * FROM finance_refunds WHERE id = :id"
                + (forUpdate ? " FOR UPDATE" : ""), Map.of("id", id), this::mapRefund).stream().findFirst();
    }

    @Override
    public List<FinanceRefundRecord> listRefunds(String merchantId, String projectId) {
        var params = new MapSqlParameterSource();
        String scope = "";
        if (merchantId != null) {
            scope += " AND merchant_id = :merchant";
            params.addValue("merchant", merchantId);
        }
        if (projectId != null) {
            scope += " AND project_id = :project";
            params.addValue("project", projectId);
        }
        return jdbcTemplate.query("SELECT * FROM finance_refunds WHERE 1=1" + scope
                + " ORDER BY created_at DESC, id", params, this::mapRefund);
    }

    @Override
    public List<FinanceRefundRecord> listRefundsForProjects(String merchantId, java.util.Set<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) {
            return List.of();
        }
        var params = new MapSqlParameterSource().addValue("merchant", merchantId, java.sql.Types.VARCHAR)
                .addValue("projects", projectIds);
        return jdbcTemplate.query("SELECT * FROM finance_refunds WHERE (:merchant IS NULL OR merchant_id = :merchant)"
                + (projectIds == null ? "" : " AND project_id IN (:projects)")
                + " ORDER BY created_at DESC, id", params, this::mapRefund);
    }

    @Override
    public List<ArApPaymentRecord> listArApPayments(String itemId) {
        return jdbcTemplate.query("SELECT * FROM ar_ap_payments WHERE ar_ap_item_id = :id ORDER BY payment_time DESC, id DESC",
                Map.of("id", itemId), this::mapArApPayment);
    }

    @Override
    public void appendRefundAttempt(FinanceRefundAttemptRecord attempt) {
        jdbcTemplate.update("""
                INSERT INTO finance_refund_attempts
                    (id, refund_id, operation, status, gateway_provider, gateway_ref, message,
                     payment_reference, payment_operator_id, verified_by, evidence_attachment_ids, created_at, dispatch_state)
                VALUES (:id, :refund, :operation, :status, :provider, :reference, :message,
                        :payment_reference, :operator, :verifier, CAST(:evidence AS jsonb), :created_at,
                        CASE WHEN :operation IN ('submit', 'retry') THEN 'ready' ELSE NULL END)
                """, new MapSqlParameterSource()
                .addValue("id", attempt.id()).addValue("refund", attempt.refundId())
                .addValue("operation", attempt.operation()).addValue("status", attempt.status())
                .addValue("provider", attempt.gatewayProvider()).addValue("reference", attempt.gatewayRef())
                .addValue("message", attempt.message()).addValue("payment_reference", attempt.paymentReference())
                .addValue("operator", attempt.paymentOperatorId()).addValue("verifier", attempt.verifiedBy())
                .addValue("evidence", writeJson(attempt.evidenceAttachmentIds()))
                .addValue("created_at", timestampWithTimeZone(attempt.createdAt())));
    }

    @Override
    public boolean claimRefundAttempt(String attemptId) {
        return jdbcTemplate.update("UPDATE finance_refund_attempts SET dispatch_state = 'claimed' "
                + "WHERE id = :id AND dispatch_state = 'ready'", Map.of("id", attemptId)) == 1;
    }

    @Override
    public Optional<FinanceRefundAttemptRecord> findReadyRefundAttempt(String refundId) {
        var ids = jdbcTemplate.queryForList("SELECT id FROM finance_refund_attempts "
                + "WHERE refund_id = :id AND dispatch_state = 'ready' ORDER BY created_at, id",
                Map.of("id", refundId), String.class);
        return listRefundAttempts(refundId).stream().filter(attempt -> ids.contains(attempt.id())).findFirst();
    }

    @Override
    public List<FinanceRefundAttemptRecord> listRefundAttempts(String refundId) {
        return jdbcTemplate.query("SELECT * FROM finance_refund_attempts WHERE refund_id = :id ORDER BY created_at, id",
                Map.of("id", refundId), (rs, row) -> new FinanceRefundAttemptRecord(
                        rs.getString("id"), rs.getString("refund_id"), rs.getString("operation"),
                        rs.getString("status"), rs.getString("gateway_provider"), rs.getString("gateway_ref"),
                        rs.getString("message"), rs.getString("payment_reference"),
                        rs.getString("payment_operator_id"), rs.getString("verified_by"),
                        readStringList(rs.getString("evidence_attachment_ids")), nullableInstant(rs, "created_at")));
    }

    private FinanceRefundRecord mapRefund(ResultSet rs, int row) throws SQLException {
        return new FinanceRefundRecord(rs.getString("id"), rs.getString("target_id"),
                rs.getLong("refund_amount_cent"), rs.getString("reason"), rs.getString("refund_channel"),
                rs.getString("refund_direction"), rs.getString("status"), nullableInstant(rs, "created_at"),
                rs.getString("source_type"), rs.getString("merchant_id"), rs.getString("project_id"),
                rs.getString("requested_by"), rs.getString("original_payment_ref"),
                rs.getString("gateway_provider"), rs.getString("gateway_ref"),
                nullableInstant(rs, "updated_at"), nullableInstant(rs, "succeeded_at"));
    }

    @Override
    public List<ReconciliationBatchRecord> listReconciliationBatches(String merchantId, java.util.Set<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        return jdbcTemplate.query("""
                SELECT b.id, b.batch_no, b.scope_type, b.scope_id, b.business_date, b.status, b.created_at
                  FROM finance_reconciliation_batches b
                """ + reconciliationScopeWhere(projectIds) + """
                 ORDER BY b.created_at DESC, b.id
                """, new MapSqlParameterSource().addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_ids", projectIds), this::mapReconciliationBatch);
    }

    /**
     * 第十八批 Y 段第 180 条 180-R2：对账批次（别名 b）的读范围谓词，同 {@link #listDailyCloses}——merchant_id 非空只到该商户的 merchant、bar 范围；
     * projectIds 非空只到这些项目的 project 范围与项目下商户的 merchant、bar 范围。
     */
    private static String reconciliationScopeWhere(java.util.Set<String> projectIds) {
        return " WHERE (:merchant_id IS NULL OR (b.scope_type IN ('merchant', 'bar') AND b.scope_id = :merchant_id))"
                + (projectIds == null ? "" : """
                   AND ((b.scope_type = 'project' AND b.scope_id IN (:project_ids))
                        OR (b.scope_type IN ('merchant', 'bar') AND b.scope_id IN (SELECT id FROM merchants WHERE project_id IN (:project_ids))))
                """);
    }

    @Override
    public ReconciliationBatchRecord saveReconciliationBatch(ReconciliationBatchRecord batch) {
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_batches (
                    id, batch_no, scope_type, scope_id, business_date, status, created_at
                )
                VALUES (
                    :id, :batch_no, :scope_type, :scope_id, :business_date, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    batch_no = EXCLUDED.batch_no,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    business_date = EXCLUDED.business_date,
                    status = EXCLUDED.status
                """, reconciliationBatchParameters(batch));
        return batch;
    }

    @Override
    public List<ReconciliationDifferenceRecord> listReconciliationDifferences(String merchantId, java.util.Set<String> projectIds) {
        if (projectIds != null && projectIds.isEmpty()) return List.of();
        return jdbcTemplate.query("""
                SELECT d.id, d.batch_id, d.difference_type, d.business_ref, d.amount_cent,
                       d.expected_amount_cent, d.actual_amount_cent, d.source, d.status, d.created_at, d.evidence_attachment_ids,
                       d.responsible_user_id, d.stage_entered_at, d.identified_cause, d.review_id,
                       CASE d.status WHEN 'pending_identification' THEN d.identify_escalated_at
                                     WHEN 'pending_handling' THEN d.handle_escalated_at END AS stage_escalated_at
                  FROM finance_reconciliation_differences d
                  JOIN finance_reconciliation_batches b ON b.id = d.batch_id
                """ + reconciliationScopeWhere(projectIds) + """
                 ORDER BY d.created_at DESC, d.id
                """, new MapSqlParameterSource().addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_ids", projectIds), this::mapReconciliationDifference);
    }

    @Override
    public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference) {
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_differences (
                    id, batch_id, difference_type, business_ref, amount_cent,
                    expected_amount_cent, actual_amount_cent, source, status, created_at, evidence_attachment_ids,
                    responsible_user_id, stage_entered_at, identified_cause, review_id
                )
                VALUES (
                    :id, :batch_id, :difference_type, :business_ref, :amount_cent,
                    :expected_amount_cent, :actual_amount_cent, :source, :status, :created_at, CAST(:evidence_attachment_ids AS jsonb),
                    :responsible_user_id, :stage_entered_at, :identified_cause, :review_id
                )
                ON CONFLICT (id) DO UPDATE SET
                    batch_id = EXCLUDED.batch_id,
                    difference_type = EXCLUDED.difference_type,
                    business_ref = EXCLUDED.business_ref,
                    amount_cent = EXCLUDED.amount_cent,
                    expected_amount_cent = EXCLUDED.expected_amount_cent,
                    actual_amount_cent = EXCLUDED.actual_amount_cent,
                    source = EXCLUDED.source,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    responsible_user_id = EXCLUDED.responsible_user_id,
                    stage_entered_at = EXCLUDED.stage_entered_at,
                    identified_cause = EXCLUDED.identified_cause,
                    review_id = EXCLUDED.review_id
                """, reconciliationDifferenceParameters(difference));
        return difference;
    }

    @Override
    public Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id) {
        return jdbcTemplate.query("""
                SELECT id, batch_id, difference_type, business_ref, amount_cent,
                       expected_amount_cent, actual_amount_cent, source, status, created_at, evidence_attachment_ids,
                       responsible_user_id, stage_entered_at, identified_cause, review_id,
                       CASE status WHEN 'pending_identification' THEN identify_escalated_at
                                   WHEN 'pending_handling' THEN handle_escalated_at END AS stage_escalated_at
                  FROM finance_reconciliation_differences
                 WHERE id = :id
                """, Map.of("id", id), this::mapReconciliationDifference).stream().findFirst();
    }

    /** 第十八批 Y 段第 181 条 181-R1：四态各步都是带原状态条件的单句更新，并发时只赢一次。 */
    @Override
    public boolean identifyReconciliationDifference(String id, String cause, String responsibleUserId, java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE finance_reconciliation_differences
                   SET status = 'pending_handling', identified_cause = :cause, responsible_user_id = :responsible_user_id,
                       stage_entered_at = :now
                 WHERE id = :id AND status = 'pending_identification'
                """, new MapSqlParameterSource().addValue("id", id).addValue("cause", cause)
                .addValue("responsible_user_id", responsibleUserId).addValue("now", timestampWithTimeZone(now))) == 1;
    }

    @Override
    public boolean submitReconciliationDifferenceReview(String id, String closeReason, List<String> evidenceAttachmentIds, String reviewId,
            java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE finance_reconciliation_differences
                   SET status = 'pending_review', close_reason = :close_reason, evidence_attachment_ids = CAST(:evidence AS jsonb),
                       review_id = :review_id, stage_entered_at = :now
                 WHERE id = :id AND status = 'pending_handling'
                """, new MapSqlParameterSource().addValue("id", id).addValue("close_reason", closeReason)
                .addValue("evidence", writeJson(evidenceAttachmentIds)).addValue("review_id", reviewId)
                .addValue("now", timestampWithTimeZone(now))) == 1;
    }

    @Override
    public Optional<String> closeReconciliationDifference(String id, String reviewId, String closedBy, java.time.Instant closedAt) {
        return jdbcTemplate.queryForList("""
                UPDATE finance_reconciliation_differences
                   SET status = 'closed', closed_by = :closed_by, closed_at = :closed_at, stage_entered_at = :closed_at
                 WHERE id = :id AND status = 'pending_review' AND review_id = :review_id
                RETURNING COALESCE(close_reason, '')
                """, new MapSqlParameterSource().addValue("id", id).addValue("review_id", reviewId)
                .addValue("closed_by", closedBy).addValue("closed_at", timestampWithTimeZone(closedAt)), String.class)
                .stream().findFirst();
    }

    @Override
    public boolean returnReconciliationDifferenceToHandling(String id, String reviewId, java.time.Instant now) {
        return jdbcTemplate.update("""
                UPDATE finance_reconciliation_differences
                   SET status = 'pending_handling', review_id = NULL, stage_entered_at = :now, handle_escalated_at = NULL
                 WHERE id = :id AND status = 'pending_review' AND review_id = :review_id
                """, new MapSqlParameterSource().addValue("id", id).addValue("review_id", reviewId)
                .addValue("now", timestampWithTimeZone(now))) == 1;
    }

    /** 第十八批 Y 段第 181 条 181-R2：项目已配置的阶段停留时限。 */
    @Override
    public List<ReconciliationStageLimit> listReconciliationStageLimits(String projectId) {
        return jdbcTemplate.query("""
                SELECT project_id, stage, working_days, min_working_days, max_working_days
                  FROM finance_reconciliation_stage_limits
                 WHERE project_id = :project_id
                """, Map.of("project_id", projectId), (rs, rowNumber) -> new ReconciliationStageLimit(rs.getString("project_id"),
                rs.getString("stage"), rs.getInt("working_days"), rs.getInt("min_working_days"), rs.getInt("max_working_days")));
    }

    @Override
    public void saveReconciliationStageLimit(ReconciliationStageLimit limit, String actorId) {
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_stage_limits (project_id, stage, working_days, min_working_days, max_working_days,
                    updated_by, updated_at)
                VALUES (:project_id, :stage, :working_days, :min_working_days, :max_working_days, :actor, now())
                ON CONFLICT (project_id, stage) DO UPDATE SET working_days = EXCLUDED.working_days,
                    min_working_days = EXCLUDED.min_working_days, max_working_days = EXCLUDED.max_working_days,
                    updated_by = EXCLUDED.updated_by, updated_at = now()
                """, new MapSqlParameterSource().addValue("project_id", limit.projectId()).addValue("stage", limit.stage())
                .addValue("working_days", limit.workingDays()).addValue("min_working_days", limit.minWorkingDays())
                .addValue("max_working_days", limit.maxWorkingDays()).addValue("actor", actorId));
    }

    /**
     * 181-R2：待识别或待处理、本阶段还没升级过的差异，带所属项目该阶段已配的时限——项目经批次取（project 范围取 scope_id，merchant、bar 范围取该商户
     * 所属项目，同复核单可见范围）。
     */
    @Override
    public List<ReconciliationEscalationCandidate> listReconciliationEscalationCandidates() {
        return jdbcTemplate.query("""
                SELECT d.id, d.status, d.responsible_user_id, d.stage_entered_at, l.working_days
                  FROM finance_reconciliation_differences d
                  JOIN finance_reconciliation_batches b ON b.id = d.batch_id
                  LEFT JOIN merchants m ON m.id = b.scope_id AND b.scope_type IN ('merchant', 'bar')
                  LEFT JOIN finance_reconciliation_stage_limits l
                         ON l.project_id = CASE WHEN b.scope_type = 'project' THEN b.scope_id ELSE m.project_id END AND l.stage = d.status
                 WHERE d.responsible_user_id IS NOT NULL AND d.stage_entered_at IS NOT NULL
                   AND ((d.status = 'pending_identification' AND d.identify_escalated_at IS NULL)
                        OR (d.status = 'pending_handling' AND d.handle_escalated_at IS NULL))
                """, (rs, rowNumber) -> new ReconciliationEscalationCandidate(rs.getString("id"), rs.getString("status"),
                rs.getString("responsible_user_id"), nullableInstant(rs, "stage_entered_at"), rs.getObject("working_days", Integer.class)));
    }

    /** 181-R2：本阶段超时升级，带原状态与「本阶段未升级过」条件，只赢一次；上一级为空时负责人不变。 */
    @Override
    public boolean markReconciliationDifferenceEscalated(String id, String stage, String superiorId, java.time.Instant now) {
        String column = "pending_identification".equals(stage) ? "identify_escalated_at" : "handle_escalated_at";
        return jdbcTemplate.update("UPDATE finance_reconciliation_differences SET " + column + " = :now,"
                + " responsible_user_id = COALESCE(:superior_id, responsible_user_id)"
                + " WHERE id = :id AND status = :stage AND " + column + " IS NULL", new MapSqlParameterSource().addValue("id", id)
                .addValue("stage", stage).addValue("superior_id", superiorId, java.sql.Types.VARCHAR)
                .addValue("now", timestampWithTimeZone(now))) == 1;
    }

    @Override
    public Optional<ReconciliationBatchRecord> findReconciliationBatchById(String id) {
        return jdbcTemplate.query("""
                SELECT id, batch_no, scope_type, scope_id, business_date, status, created_at
                  FROM finance_reconciliation_batches
                 WHERE id = :id
                """, Map.of("id", id), this::mapReconciliationBatch).stream().findFirst();
    }

    @Override
    public long aggregatePlatformExpectedForBusinessRef(String businessRef) {
        // 平台侧 expected 系统取数：以 business_ref 关联平台权威支付台账（ar_ap_payments.id），
        // 聚合该笔的已登记金额作为平台侧应有值。无对应平台流水 → 返回 0（服务层据此判定无系统取数支撑）。
        // 第十八批 Y 段第 180 条 180-R3（ENUM:188「对账口径 按订单、按核销、按合同、按渠道、按日结」）：按订单口径——收款记录查不到再查
        // 已支付票务订单（取订单金额），再查酒吧账单（取已付金额）；三表编号互不相同。18Y 复核修复：票务订单、酒吧账单另认页面上显示的订单号（order_no）、
        // 账单号（bill_no），两列都唯一。
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(
                       (SELECT SUM(amount_cent) FROM ar_ap_payments WHERE id = :business_ref),
                       (SELECT amount_cent FROM ticket_orders WHERE (id = :business_ref OR order_no = :business_ref) AND pay_status = 'paid'),
                       (SELECT paid_amount_cent FROM bar_bills WHERE id = :business_ref OR bill_no = :business_ref),
                       0)
                """, Map.of("business_ref", businessRef), Long.class);
        return total == null ? 0L : total;
    }

    @Override
    public List<String> listReconciliationDifferenceIdsByEvidenceAttachment(String attachmentId) {
        return jdbcTemplate.queryForList("""
                SELECT id FROM finance_reconciliation_differences
                 WHERE evidence_attachment_ids @> jsonb_build_array(CAST(:attachment_id AS text))
                """, Map.of("attachment_id", attachmentId), String.class);
    }

    // ── R5 T7：日结锁定 + 补录审批 + 多源汇总 ──────────────────────────────

    @Override
    public Optional<DailyCloseRecord> findDailyCloseById(String id) {
        return jdbcTemplate.query("""
                SELECT id, business_date, scope_type, scope_id, status, locked_at, created_at, confirmed_at, archived_at,
                       (SELECT string_agg(m.category, ',' ORDER BY m.marked_at, m.category)
                          FROM finance_daily_close_final_marks m
                         WHERE m.daily_close_id = finance_daily_closes.id) AS final_mark_categories
                  FROM finance_daily_closes
                 WHERE id = :id
                """, Map.of("id", id), this::mapDailyClose).stream().findFirst();
    }

    /** 第十八批 R 段 143-R2：五类最终标记一类一行（主键日结＋类别）。 */
    @Override
    public void saveDailyCloseFinalMark(String dailyCloseId, String category, Instant markedAt) {
        jdbcTemplate.update("""
                INSERT INTO finance_daily_close_final_marks (daily_close_id, category, marked_at)
                VALUES (:daily_close_id, :category, :marked_at)
                """, new MapSqlParameterSource()
                .addValue("daily_close_id", dailyCloseId)
                .addValue("category", category)
                .addValue("marked_at", timestampWithTimeZone(markedAt)));
    }

    @Override
    public DailyCloseSummaryRecord saveDailyCloseSummary(DailyCloseSummaryRecord summary) {
        jdbcTemplate.update("""
                INSERT INTO finance_daily_close_summaries (
                    id, daily_close_id, source_stream, amount_cent, refund_cent, order_count, created_at
                )
                VALUES (
                    :id, :daily_close_id, :source_stream, :amount_cent, :refund_cent, :order_count, :created_at
                )
                ON CONFLICT (daily_close_id, source_stream) DO UPDATE SET
                    amount_cent = EXCLUDED.amount_cent,
                    refund_cent = EXCLUDED.refund_cent,
                    order_count = EXCLUDED.order_count
                """, new MapSqlParameterSource()
                .addValue("id", summary.id())
                .addValue("daily_close_id", summary.dailyCloseId())
                .addValue("source_stream", summary.sourceStream())
                .addValue("amount_cent", summary.amountCent())
                .addValue("refund_cent", summary.refundCent())
                .addValue("order_count", summary.orderCount())
                .addValue("created_at", timestampWithTimeZone(summary.createdAt())));
        return summary;
    }

    @Override
    public List<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId) {
        return jdbcTemplate.query("""
                SELECT id, daily_close_id, source_stream, amount_cent, refund_cent, order_count, created_at
                  FROM finance_daily_close_summaries
                 WHERE daily_close_id = :daily_close_id
                 ORDER BY source_stream
                """, Map.of("daily_close_id", dailyCloseId), this::mapDailyCloseSummary);
    }

    @Override
    public com.complex.platform.common.api.PageResponse<DailyCloseRecord> listDailyCloses(
            String merchantId, java.util.Set<String> projectIds, LocalDate businessDate, String scopeType, String scopeId,
            int page, int size) {
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 200);
        if (projectIds != null && projectIds.isEmpty())
            return new com.complex.platform.common.api.PageResponse<>(List.of(), safePage, safeSize, 0L);
        // 同一个筛选谓词用于行和总数：商户只到本商户的 merchant、bar 范围；项目集合到项目范围与项目下商户。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_ids", projectIds)
                .addValue("business_date", businessDate)
                .addValue("scope_type", scopeType)
                .addValue("scope_id", scopeId);
        // 第十八批 R 段 143-R1：营业日、范围类型、范围编号非空时再按它们过滤（页面查「待生成」）。
        String where = " WHERE (:merchant_id IS NULL OR (scope_type IN ('merchant', 'bar') AND scope_id = :merchant_id))"
                + (businessDate == null ? "" : " AND business_date = :business_date")
                + (scopeType == null ? "" : " AND scope_type = :scope_type")
                + (scopeId == null ? "" : " AND scope_id = :scope_id")
                + (projectIds == null ? "" : """
                   AND ((scope_type = 'project' AND scope_id IN (:project_ids))
                        OR (scope_type IN ('merchant', 'bar') AND scope_id IN (SELECT id FROM merchants WHERE project_id IN (:project_ids))))
                """);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM finance_daily_closes" + where, params, Long.class);
        params.addValue("limit", safeSize).addValue("offset", (long) (safePage - 1) * safeSize);
        List<DailyCloseRecord> items = jdbcTemplate.query("""
                SELECT id, business_date, scope_type, scope_id, status, locked_at, created_at, confirmed_at, archived_at,
                       (SELECT string_agg(m.category, ',' ORDER BY m.marked_at, m.category)
                          FROM finance_daily_close_final_marks m
                         WHERE m.daily_close_id = finance_daily_closes.id) AS final_mark_categories
                  FROM finance_daily_closes""" + where + """
                 ORDER BY business_date DESC, created_at DESC, id
                 LIMIT :limit OFFSET :offset
                """, params, this::mapDailyClose);
        return new com.complex.platform.common.api.PageResponse<>(items, safePage, safeSize, total == null ? 0L : total);
    }

    /** 第十八批 V 段第 159 条 159-R1：检查单位（两条 SQL 由 FinanceFinalMarkOverdueService 原样搬来，排序不变）。 */
    @Override
    public List<FinalMarkUnit> listFinalMarkUnits() {
        List<FinalMarkUnit> units = new java.util.ArrayList<>(jdbcTemplate.query(
                "SELECT id, merchant_name, project_id, created_at FROM merchants WHERE merchant_type = 'bar' AND status = 'active' "
                        + "AND archived_at IS NULL ORDER BY id",
                Map.of(), (rs, i) -> new FinalMarkUnit(rs.getString(1), rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getTimestamp(4).toInstant())));
        units.addAll(jdbcTemplate.query("SELECT id, name, created_at FROM projects WHERE status = 'active' ORDER BY id",
                Map.of(), (rs, i) -> new FinalMarkUnit(null, rs.getString(1), rs.getString(2), rs.getString(1),
                        rs.getTimestamp(3).toInstant())));
        return units;
    }

    /** 159-R1：酒吧取 bar、merchant 范围日结，否则取 project 范围日结；同日多张取并集，没有标记的营业日没有键。 */
    @Override
    public Map<LocalDate, java.util.Set<String>> finalMarkCategoriesByDay(String barId, String projectId, LocalDate from,
            LocalDate to) {
        Map<LocalDate, java.util.Set<String>> result = new java.util.HashMap<>();
        jdbcTemplate.query("SELECT c.business_date, m.category FROM finance_daily_closes c"
                        + " JOIN finance_daily_close_final_marks m ON m.daily_close_id = c.id"
                        + (barId != null ? " WHERE c.scope_type IN ('merchant', 'bar')" : " WHERE c.scope_type = 'project'")
                        + " AND c.scope_id = :scope_id AND c.business_date BETWEEN :from AND :to",
                new MapSqlParameterSource().addValue("scope_id", barId != null ? barId : projectId)
                        .addValue("from", from).addValue("to", to),
                rs -> {
                    result.computeIfAbsent(rs.getObject(1, LocalDate.class), day -> new java.util.HashSet<>()).add(rs.getString(2));
                });
        return result;
    }

    @Override
    public int lockArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String merchantId) {
        // 经营日范围：以 due_date 当日（AR/AP 业务日）锁定本批次台账。merchantId 非空时仅锁本商户行（隔离）。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("daily_close_id", dailyCloseId)
                .addValue("business_date", businessDate)
                // merchantId 可为 null(平台口径):必须带 SQL 类型,否则 PG 无法推断 :merchant_id IS NULL 的参数类型直接 500。
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        return jdbcTemplate.update("""
                UPDATE ar_ap_items
                   SET locked_day_close_id = :daily_close_id
                 WHERE due_date = :business_date
                   AND locked_day_close_id IS NULL
                   AND (:merchant_id IS NULL OR merchant_id = :merchant_id)
                """, params);
    }

    /** 第十八批 S 段第 161 条 161-R2：综合体日结打锁，口径见接口（派生台账多数 project_id 为空，用商户所属综合体补）。 */
    @Override
    public int lockProjectArApItemsForDailyClose(String dailyCloseId, LocalDate businessDate, String projectId) {
        return jdbcTemplate.update("""
                UPDATE ar_ap_items a
                   SET locked_day_close_id = :daily_close_id
                 WHERE a.due_date = :business_date
                   AND a.locked_day_close_id IS NULL
                   AND COALESCE(a.project_id, (SELECT m.project_id FROM merchants m WHERE m.id = a.merchant_id)) = :project_id
                   AND NOT EXISTS (SELECT 1 FROM merchants m WHERE m.id = a.merchant_id AND m.merchant_type = 'bar')
                """, new MapSqlParameterSource()
                .addValue("daily_close_id", dailyCloseId)
                .addValue("business_date", businessDate)
                .addValue("project_id", projectId));
    }

    @Override
    public Optional<String> findArApItemLockedDayCloseId(String arApItemId) {
        List<String> rows = jdbcTemplate.query("""
                SELECT locked_day_close_id
                  FROM ar_ap_items
                 WHERE id = :id
                """, Map.of("id", arApItemId),
                (rs, rowNum) -> rs.getString("locked_day_close_id"));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rows.get(0));
    }

    @Override
    public boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId) {
        return lockedDailyCloseExistsForDate(businessDate, merchantId, null);
    }

    @Override
    public boolean lockedDailyCloseExistsForDate(LocalDate businessDate, String merchantId, String projectId) {
        // F-OPS-09：商户级日结锁。与 lockArApItemsForDailyClose 同口径——
        // merchantId 非空（商户 scope 调用方）：仅本商户封账（scope_type 为 merchant 或 bar 且 scope_id=:merchant_id）拦截，
        //   他商户封账不冻结本商户写入；第十八批 S 段第 161 条 161-R1（ENUM:190「日结锁定后的补录须上级审批，且不改动已锁定的日结金额」；
        //   PGM:664 单店日结由该酒吧对应的财务 BP 负责）：bar 范围与 merchant 范围同把 scope_id 当该酒吧商户（同 143-R7 打锁），
        //   平台侧财务锁某家酒吧 bar 范围的日结同样冻结该酒吧商户当日的写入；
        // merchantId 为空（平台口径调用方）：该经营日任一 locked 日结即拦截（平台口径写入受所有封账约束）。
        // 第十八批 R 段 143-R5（第十八批裁决 33「归档后只读可查，锁定后补录照常走上级审批。」）：已归档的日结同样封账。
        // 第十八批 S 段第 161 条 161-R2（第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：商户不是酒吧、能定综合体（projectId，
        //   空则取商户所属项目）的，另受该综合体项目范围日结约束；只有商户与项目都定不出时才受任一封账约束。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                // merchantId 可为 null(平台口径):必须带 SQL 类型,否则 PG 无法推断 :merchant_id IS NULL 的参数类型直接 500。
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR);
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM finance_daily_closes d
                  CROSS JOIN (SELECT COALESCE(CAST(:project_id AS VARCHAR), m.project_id) AS project_id,
                                     COALESCE(m.merchant_type = 'bar', FALSE) AS is_bar
                                FROM (SELECT 1) one
                                LEFT JOIN merchants m ON m.id = :merchant_id) u
                 WHERE d.business_date = :business_date
                   AND d.status IN ('locked', 'archived')
                   AND ((d.scope_type IN ('merchant', 'bar') AND d.scope_id = :merchant_id)
                        OR (u.project_id IS NULL AND CAST(:merchant_id AS VARCHAR) IS NULL)
                        OR (NOT u.is_bar AND d.scope_type = 'project' AND d.scope_id = u.project_id))
                """, params, Integer.class);
        return count != null && count > 0;
    }

    // OBS-13-07（BSM L1681 官方 App/小程序与第三方渠道分开汇总）：自营售票渠道，即 TicketOrderService.SALES_CHANNELS 去掉 channel
    // 与各第三方渠道码；第三方渠道回流出的票 sales_channel 记具体渠道码（meituan、douyin 等），一律归渠道售票源。
    // 第十八批第 3 条：与结算「官方渠道不扣销售渠道费」共用 TicketOrderService.OFFICIAL_SALES_CHANNELS 一处定义。
    private static final String SELF_SALES_CHANNELS = "('" + String.join("', '",
            new java.util.TreeSet<>(com.complex.platform.ticketing.TicketOrderService.OFFICIAL_SALES_CHANNELS)) + "')";

    @Override
    public DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 官方售票：ticket_orders 已支付(paid)，以 visit_date 为经营日；金额=实付分，count=单数。
        // 仅归集自营售票（SELF_SALES_CHANNELS）；成功退款按 succeeded_at 落在经营日窗口 [from, to) 冲减本源收入。
        // 财务确认：revenue.net = 自营售票 + 酒吧 − 退款（不含第三方回流）；退款冲减按经营日发生。
        return aggregateTotals("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel IN %1$s
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel IN %1$s
                   AND o.visit_date = :business_date
                """.formatted(SELF_SALES_CHANNELS), businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // P1#15：第三方渠道售票接入真实数据源。ticket_orders.sales_channel 不在 SELF_SALES_CHANNELS（字面 channel 及 meituan、
        // douyin 等渠道码，第三方渠道回流本地出票记具体渠道码；OBS-13-07）已支付(paid)，以 visit_date 为经营日；金额=实付分，count=单数。
        // 退款（冲减当日净收入）：ticket_refunds 按渠道售票订单、created_at 落经营日窗口 [from, to) 归集为 refund_cent。
        // 跨域只读：仅读 ticketing 域既有表，不改其写。
        return aggregateTotals("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel NOT IN %1$s
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel NOT IN %1$s
                   AND o.visit_date = :business_date
                """.formatted(SELF_SALES_CHANNELS), businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateBar(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        return aggregateBar(businessDate, fromInclusive, toExclusive, null, null, Map.of());
    }

    @Override
    public DailyCloseSourceTotals aggregateAmusement(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 游乐：amusement_verifications 成功核销计数（收入归票务，此处仅客流/核销量，不计收入）。
        return aggregateTotals("""
                SELECT 0 AS amount_cent,
                       0 AS refund_cent,
                       COUNT(*) AS order_count
                  FROM amusement_verifications
                 WHERE verification_result = 'success'
                   AND created_at >= :from
                   AND created_at < :to
                """, businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateSupplyCost(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 供应链成本侧：inventory_stock_movements 当日出库消耗笔数（成本/库存影响，不计经营收入）。
        return aggregateTotals("""
                SELECT 0 AS amount_cent,
                       0 AS refund_cent,
                       COUNT(*) AS order_count
                  FROM inventory_stock_movements
                 WHERE created_at >= :from
                   AND created_at < :to
                """, businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateThirdpartyReflow(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 第三方回流（餐饮/零售）：只计已入账（sync_type='order'、status='synced'）且已按门店映射落商户的餐饮/零售回流订单，
        // 以 created_at 落在经营日窗口归集；谓词与 analytics THIRDPARTY_REVENUE_WHERE 一致（OBS-13-07）。
        // 渠道团购票（business_type='ticket'）已本地出票、计入渠道售票源；映射缺失（merchant_id 为空）按 BSM L1548 挂差异、不入账。
        // 财务确认：第三方回流仅作商户结算口径单列展示，不计入平台 revenue.net（净收入派生时剔除本源）。
        return aggregateTotals("""
                SELECT COALESCE(SUM(amount_cent), 0) AS amount_cent,
                       0 AS refund_cent,
                       COUNT(*) AS order_count
                  FROM integration_order_syncs
                 WHERE sync_type = 'order'
                   AND status = 'synced'
                   AND business_type IN ('catering', 'retail')
                   AND merchant_id IS NOT NULL
                   AND created_at >= :from
                   AND created_at < :to
                """, businessDate, fromInclusive, toExclusive);
    }

    // ── P2-A：六源汇总按 scope 取数(与 lockArApItemsForDailyClose 口径对齐)──────────
    // 口径:scopeType 为 merchant 或 bar 时按 merchant 维切片(源表有 merchant_id 才能归属,无则归零);
    //       第十八批 S 段第 161 条 161-R1:bar 范围与 merchant 范围同把 scope_id 当该酒吧商户(同 dailyCloseProject、143-R7 打锁),
    //       七源汇总与日结各闸(票务核销异常、酒吧账单异常、未结清账单、班次交接、有差异、对账差异)都只取该酒吧;
    //       第十八批 S 段第 161 条 161-R2:project 范围＝该综合体除酒吧外的业务(第十八批裁决 35),酒吧与订台定金两源归零,门票按售票项目快照、
    //       游乐按游乐项目所属商户、供应链按流水项目(空取商户所属)、第三方回流按回流门店所属综合体,日结闸里只属酒吧的三道不看;
    //       第十八批 S 段第 161 条 161-R3:日结范围只有 project、merchant、bar,不再有 ticketing、amusement 范围。
    //       第十八批 S 段第 161 条复核：merchant、bar 范围门票按受理商户或绑定酒吧取该酒吧（客人自助、渠道回流的单店票受理商户为空，BSM:749），
    //       供应链按流水商户取该酒吧。
    // 无 merchant 维的源(amusement/thirdparty_reflow)在 merchant scope 下归零,不再渗入全平台。
    private static final DailyCloseSourceTotals ZERO = DailyCloseSourceTotals.empty();

    private boolean isMerchantScope(String scopeType) {
        return "merchant".equals(scopeType) || "bar".equals(scopeType);
    }

    /**
     * 门票单按范围归属：merchant、bar 范围归本商户，受理商户或绑定酒吧（V288）为本酒吧的都算（客人自助、渠道回流的单店票受理商户为空，
     * BSM:749 归该酒吧）；161-R2 project 范围取售票项目快照（V463）为该综合体、且售票商户与绑定酒吧都不是酒吧的单（单店酒吧门票随酒吧日结）。
     */
    private static String ticketOrderInScope(String alias, String scopeType) {
        return "project".equals(scopeType)
                ? alias + ".project_id = :scope_id AND NOT EXISTS (SELECT 1 FROM merchants bm WHERE bm.id IN (" + alias
                        + ".merchant_id, " + alias + ".bar_id) AND bm.merchant_type = 'bar')"
                : "(" + alias + ".merchant_id = :merchant_id OR " + alias + ".bar_id = :merchant_id)";
    }

    private DailyCloseSourceTotals aggregateTotalsScoped(
            String sql, LocalDate businessDate, Instant from, Instant to, String scopeId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("from", timestampWithTimeZone(from))
                .addValue("to", timestampWithTimeZone(to))
                .addValue("merchant_id", scopeId, java.sql.Types.VARCHAR)
                .addValue("scope_id", scopeId, java.sql.Types.VARCHAR);  // 161-R2：project 范围的 SQL 用 :scope_id
        return jdbcTemplate.query(sql, params, (rs) -> {
            if (!rs.next()) {
                return DailyCloseSourceTotals.empty();
            }
            return new DailyCloseSourceTotals(
                    rs.getLong("amount_cent"), rs.getLong("refund_cent"), rs.getInt("order_count"));
        });
    }

    @Override
    public DailyCloseSourceTotals aggregateOfficialTicket(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        if (!isMerchantScope(scopeType) && !"project".equals(scopeType)) {
            return aggregateOfficialTicket(businessDate, from, to);
        }
        // ticket_orders 有 merchant_id:merchant scope 仅归本商户;退款按本商户订单归集。
        return aggregateTotalsScoped("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel IN %1$s
                           AND %2$s
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel IN %1$s
                   AND o.visit_date = :business_date
                   AND %3$s
                """.formatted(SELF_SALES_CHANNELS, ticketOrderInScope("ro", scopeType), ticketOrderInScope("o", scopeType)),
                businessDate, from, to, scopeId);
    }

    @Override
    public DailyCloseSourceTotals aggregateChannelTicket(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        if (!isMerchantScope(scopeType) && !"project".equals(scopeType)) {
            return aggregateChannelTicket(businessDate, from, to);
        }
        return aggregateTotalsScoped("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel NOT IN %1$s
                           AND %2$s
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel NOT IN %1$s
                   AND o.visit_date = :business_date
                   AND %3$s
                """.formatted(SELF_SALES_CHANNELS, ticketOrderInScope("ro", scopeType), ticketOrderInScope("o", scopeType)),
                businessDate, from, to, scopeId);
    }

    @Override
    public DailyCloseSourceTotals aggregateBar(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        return aggregateBar(businessDate, from, to, scopeType, scopeId, Map.of());
    }

    @Override
    public DailyCloseSourceTotals aggregateBar(LocalDate businessDate, Instant from, Instant to, String scopeType,
            String scopeId, Map<String, Instant[]> barWindows) {
        // 酒吧：bar_bill_settlements 已结算（settle_channel 全计），以 settled_at 落在所属酒吧的营业日窗归集；
        // 金额=结算分，count=结算笔数。退款：bar_bill_adjustments 中 adjustment_type='refund'（结账后退款减项）按 refunded_at
        //   落所属酒吧营业日窗合计为 refund_cent；免单 waiver 不计入（结账前已减应付，避免重复冲减）。
        // merchant scope 仅归本商户结算；退款按账单是否有本商户结算流水归属（EXISTS），不按流水逐行连接——
        //   账单有多笔收款流水（多渠道收款、作废留存已退部分）时退款不重复计（第十七批⑤）。其余 scope 平台全量。
        if ("project".equals(scopeType)) {
            return ZERO;  // 161-R2：酒吧逐店另出日结，综合体日结不含酒吧源
        }
        String merchantId = isMerchantScope(scopeType) ? scopeId : null;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("from", timestampWithTimeZone(from))
                .addValue("to", timestampWithTimeZone(to))
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        String settled = barWindowPredicate("s.settled_at",
                "(SELECT b.bar_id FROM bar_bills b WHERE b.id = s.bill_id)", barWindows, params);
        String refunded = barWindowPredicate("adj.refunded_at",
                "(SELECT b.bar_id FROM bar_bills b WHERE b.id = adj.bill_id)", barWindows, params);
        return jdbcTemplate.query("""
                SELECT (SELECT COALESCE(SUM(s.settlement_amount_cent), 0)
                          FROM bar_bill_settlements s
                         WHERE %1$s
                           AND s.reversed = FALSE
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL OR s.merchant_id = :merchant_id)) AS amount_cent,
                       (SELECT COALESCE(SUM(adj.amount_cent), 0)
                          FROM bar_bill_adjustments adj
                         WHERE adj.adjustment_type = 'refund'
                           AND %2$s
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL
                                OR EXISTS (SELECT 1 FROM bar_bill_settlements bs
                                            WHERE bs.bill_id = adj.bill_id AND bs.merchant_id = :merchant_id))) AS refund_cent,
                       (SELECT COUNT(*)
                          FROM bar_bill_settlements s
                         WHERE %1$s
                           AND s.reversed = FALSE
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL OR s.merchant_id = :merchant_id)) AS order_count
                """.formatted(settled, refunded), params, (rs) -> {
            if (!rs.next()) {
                return DailyCloseSourceTotals.empty();
            }
            return new DailyCloseSourceTotals(rs.getLong("amount_cent"), rs.getLong("refund_cent"), rs.getInt("order_count"));
        });
    }

    @Override
    public DailyCloseSourceTotals aggregateBarDeposit(LocalDate businessDate, Instant from, Instant to, String scopeType,
            String scopeId, Map<String, Instant[]> barWindows) {
        // 第十八批 O 段第 126 条 126-R1：订台定金（bar_reservations）以 deposit_paid_at 落所属酒吧营业日窗归集，金额＝定金合计、count＝笔数
        //   （各渠道；到账时点只在员工登记或线上到账时写，免收与待付为空、天然不计）。退款：已原路退回（refunded / refunded_on_threshold，
        //   deposit_settled_at 只在实际退款成功时写）以 deposit_settled_at 落窗合计。merchant scope 仅归本商户订台，其余 scope 平台全量。
        //   定金不抵扣消费、不计入酒吧源。
        if ("project".equals(scopeType)) {
            return ZERO;  // 161-R2：订台定金归酒吧，综合体日结不含
        }
        String merchantId = isMerchantScope(scopeType) ? scopeId : null;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", timestampWithTimeZone(from))
                .addValue("to", timestampWithTimeZone(to))
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        String paid = barWindowPredicate("r.deposit_paid_at", "r.bar_id", barWindows, params);
        String refunded = barWindowPredicate("r.deposit_settled_at", "r.bar_id", barWindows, params);
        return jdbcTemplate.query("""
                SELECT (SELECT COALESCE(SUM(r.deposit_amount_cent), 0)
                          FROM bar_reservations r
                         WHERE %1$s
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL OR r.merchant_id = :merchant_id)) AS amount_cent,
                       (SELECT COALESCE(SUM(r.deposit_amount_cent), 0)
                          FROM bar_reservations r
                         WHERE r.deposit_settlement IN ('refunded', 'refunded_on_threshold')
                           AND %2$s
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL OR r.merchant_id = :merchant_id)) AS refund_cent,
                       (SELECT COUNT(*)
                          FROM bar_reservations r
                         WHERE %1$s
                           AND (CAST(:merchant_id AS VARCHAR) IS NULL OR r.merchant_id = :merchant_id)) AS order_count
                """.formatted(paid, refunded), params, (rs) -> {
            if (!rs.next()) {
                return DailyCloseSourceTotals.empty();
            }
            return new DailyCloseSourceTotals(rs.getLong("amount_cent"), rs.getLong("refund_cent"), rs.getInt("order_count"));
        });
    }

    @Override
    public DailyCloseSourceTotals aggregateAmusement(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        // amusement_verifications 无 merchant 维:merchant scope 归零(不属于任一商户);其余 scope 平台全量。
        if ("project".equals(scopeType)) {
            // 161-R2：游乐项目所属商户（V049 amusement_projects.merchant_id）属该综合体的成功核销。
            return aggregateTotalsScoped("""
                    SELECT 0 AS amount_cent,
                           0 AS refund_cent,
                           COUNT(*) AS order_count
                      FROM amusement_verifications v
                      JOIN amusement_projects ap ON ap.id = v.project_id
                      JOIN merchants m ON m.id = ap.merchant_id
                     WHERE v.verification_result = 'success'
                       AND v.created_at >= :from
                       AND v.created_at < :to
                       AND m.project_id = :scope_id
                    """, businessDate, from, to, scopeId);
        }
        return isMerchantScope(scopeType) ? ZERO : aggregateAmusement(businessDate, from, to);
    }

    @Override
    public DailyCloseSourceTotals aggregateSupplyCost(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        // inventory_stock_movements 有 merchant_id（V046）:merchant、bar scope 按流水商户取本酒吧;其余 scope 平台全量。
        if (isMerchantScope(scopeType)) {
            return aggregateTotalsScoped("""
                    SELECT 0 AS amount_cent,
                           0 AS refund_cent,
                           COUNT(*) AS order_count
                      FROM inventory_stock_movements s
                     WHERE s.created_at >= :from
                       AND s.created_at < :to
                       AND s.merchant_id = :merchant_id
                    """, businessDate, from, to, scopeId);
        }
        if ("project".equals(scopeType)) {
            // 161-R2：流水项目（V244，空取商户所属）为该综合体、商户不是酒吧的出入库流水。
            return aggregateTotalsScoped("""
                    SELECT 0 AS amount_cent,
                           0 AS refund_cent,
                           COUNT(*) AS order_count
                      FROM inventory_stock_movements s
                      LEFT JOIN merchants m ON m.id = s.merchant_id
                     WHERE s.created_at >= :from
                       AND s.created_at < :to
                       AND COALESCE(s.project_id, m.project_id) = :scope_id
                       AND m.merchant_type IS DISTINCT FROM 'bar'
                    """, businessDate, from, to, scopeId);
        }
        return aggregateSupplyCost(businessDate, from, to);
    }

    @Override
    public DailyCloseSourceTotals aggregateThirdpartyReflow(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        // integration_order_syncs 无 merchant 维:merchant scope 归零;其余 scope 平台全量。
        if ("project".equals(scopeType)) {
            // 161-R2：回流门店（V334 integration_order_syncs.merchant_id）所属综合体为该综合体的已入账餐饮、零售回流（谓词同无 scope 版）。
            return aggregateTotalsScoped("""
                    SELECT COALESCE(SUM(s.amount_cent), 0) AS amount_cent,
                           0 AS refund_cent,
                           COUNT(*) AS order_count
                      FROM integration_order_syncs s
                      JOIN merchants m ON m.id = s.merchant_id
                     WHERE s.sync_type = 'order'
                       AND s.status = 'synced'
                       AND s.business_type IN ('catering', 'retail')
                       AND s.created_at >= :from
                       AND s.created_at < :to
                       AND m.project_id = :scope_id
                    """, businessDate, from, to, scopeId);
        }
        return isMerchantScope(scopeType) ? ZERO : aggregateThirdpartyReflow(businessDate, from, to);
    }

    // ── P1#13 库存差异 → AR/AP（V108）─────────────────────────────────────────

    @Override
    public Optional<InventoryVarianceRecord> findInventoryVarianceById(String id) {
        return jdbcTemplate.query("""
                SELECT v.id, v.stocktake_id, v.item_id, v.batch_no, v.book_qty, v.counted_qty,
                       v.variance_qty, v.variance_amount_cent, v.reason, v.supplier_id,
                       v.review_status, v.posted_ar_ap_item_id, s.merchant_id, v.created_at
                  FROM inventory_variance v
                  JOIN stocktake s ON s.id = v.stocktake_id
                 WHERE v.id = :id
                """, Map.of("id", id), this::mapInventoryVariance).stream().findFirst();
    }

    @Override
    public boolean markInventoryVariancePostedIfUnposted(String inventoryVarianceId, String arApItemId) {
        // P1 修复：条件 UPDATE 加 AND posted_ar_ap_item_id IS NULL。并发双开 postInventoryVariance 时，
        // 二者各自 persist 了应付台账后争相回填，只有先到者把 NULL→arApItemId（受影响 1 行返回 true），
        // 后到者命中 0 行返回 false，服务层据此抛 INVENTORY_VARIANCE_ALREADY_POSTED 冲突，避免重复挂应付。
        int affected = jdbcTemplate.update("""
                UPDATE inventory_variance
                   SET posted_ar_ap_item_id = :ar_ap_item_id,
                       updated_at = now()
                 WHERE id = :id
                   AND posted_ar_ap_item_id IS NULL
                """, new MapSqlParameterSource()
                .addValue("id", inventoryVarianceId)
                .addValue("ar_ap_item_id", arApItemId));
        return affected == 1;
    }

    private InventoryVarianceRecord mapInventoryVariance(ResultSet rs, int rowNumber) throws SQLException {
        return new InventoryVarianceRecord(
                rs.getString("id"),
                rs.getString("stocktake_id"),
                rs.getString("item_id"),
                rs.getString("batch_no"),
                rs.getBigDecimal("book_qty"),
                rs.getBigDecimal("counted_qty"),
                rs.getBigDecimal("variance_qty"),
                rs.getLong("variance_amount_cent"),
                rs.getString("reason"),
                rs.getString("supplier_id"),
                rs.getString("review_status"),
                rs.getString("posted_ar_ap_item_id"),
                rs.getString("merchant_id"),
                nullableInstant(rs, "created_at"));
    }

    // ── P1#14 综合体计费（V110）：营业额抽成 / 保证金 / CAM ───────────────────

    @Override
    public Optional<TurnoverRentRuleRecord> findTurnoverRentRuleById(String id) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, max_amount_cent,
                       effective_from, effective_to, status, created_at
                  FROM turnover_rent_rules
                 WHERE id = :id
                """, Map.of("id", id), this::mapTurnoverRentRule).stream().findFirst();
    }

    @Override
    public String findContractProjectId(String contractId) {
        return jdbcTemplate.query("SELECT project_id FROM contracts WHERE id = :id", new MapSqlParameterSource("id", contractId),
                rs -> rs.next() ? rs.getString(1) : null);
    }

    @Override
    public Optional<TurnoverRentRuleRecord> findActiveTurnoverRentRuleByContract(String contractId) {
        return jdbcTemplate.query("""
                SELECT id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, max_amount_cent,
                       effective_from, effective_to, status, created_at
                  FROM turnover_rent_rules
                 WHERE contract_id = :contract_id AND status = 'active' AND is_draft = FALSE
                 ORDER BY effective_from DESC, created_at DESC
                 LIMIT 1
                """, Map.of("contract_id", contractId), this::mapTurnoverRentRule).stream().findFirst();
    }

    @Override
    public List<TurnoverRentRuleRecord> findTurnoverRentRulesForPeriod(
            String contractId, String merchantId, LocalDate periodStart, LocalDate periodEnd) {
        return jdbcTemplate.query("""
                SELECT r.id, r.contract_id, r.merchant_id, r.rule_name, r.rate_bps,
                       r.min_amount_cent, r.max_amount_cent, r.effective_from, r.effective_to,
                       r.status, r.created_at
                  FROM turnover_rent_rules r
                 WHERE r.contract_id IS NOT DISTINCT FROM :contract_id
                   AND r.merchant_id = :merchant_id AND r.is_draft = FALSE
                   AND r.archive_reason IS NULL
                   AND r.effective_from <= :period_start
                   AND (r.effective_to IS NULL OR r.effective_to >= :period_end)
                   AND NOT EXISTS (
                       SELECT 1 FROM turnover_rent_rules newer
                        WHERE newer.contract_id IS NOT DISTINCT FROM r.contract_id
                          AND newer.merchant_id = r.merchant_id AND newer.is_draft = FALSE
                          AND newer.id <> r.id
                          AND (newer.effective_from > r.effective_from
                              OR (newer.effective_from = r.effective_from
                                  AND (newer.activation_sequence IS NULL OR r.activation_sequence IS NULL
                                      OR newer.activation_sequence > r.activation_sequence)))
                          AND newer.effective_from <= :period_end)
                """, new MapSqlParameterSource()
                .addValue("contract_id", contractId, java.sql.Types.VARCHAR)
                .addValue("merchant_id", merchantId)
                .addValue("period_start", periodStart)
                .addValue("period_end", periodEnd), this::mapTurnoverRentRule);
    }

    /**
     * P0-E：落草案（is_draft=TRUE）+ 记起草人。读侧 findActiveTurnoverRentRuleByContract 只认 is_draft=FALSE，
     * 故草案落库后对结算<b>零影响</b>，须经配置中心差人复核转正才生效。
     */
    @Override
    public TurnoverRentRuleRecord saveTurnoverRentRuleDraft(TurnoverRentRuleRecord rule, String updatedBy) {
        jdbcTemplate.update("""
                INSERT INTO turnover_rent_rules (
                    id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, max_amount_cent,
                    effective_from, effective_to, status, is_draft, updated_by, created_at, updated_at
                ) VALUES (
                    :id, :contract_id, :merchant_id, :rule_name, :rate_bps, :min_amount_cent, :max_amount_cent,
                    :effective_from, :effective_to, :status, TRUE, :updated_by, :created_at, :created_at
                )
                """, turnoverRentRuleParameters(rule).addValue("updated_by", updatedBy));
        return rule;
    }

    @Override
    public boolean isTurnoverRentRuleDraft(String id) {
        Boolean draft = jdbcTemplate.query(
                "SELECT is_draft FROM turnover_rent_rules WHERE id = :id",
                Map.of("id", id), rs -> rs.next() ? rs.getBoolean(1) : null);
        return Boolean.TRUE.equals(draft);
    }

    @Override
    public TurnoverRentRuleRecord saveTurnoverRentRule(TurnoverRentRuleRecord rule) {
        jdbcTemplate.update("""
                INSERT INTO turnover_rent_rules (
                    id, contract_id, merchant_id, rule_name, rate_bps, min_amount_cent, max_amount_cent,
                    effective_from, effective_to, status, created_at, updated_at
                ) VALUES (
                    :id, :contract_id, :merchant_id, :rule_name, :rate_bps, :min_amount_cent, :max_amount_cent,
                    :effective_from, :effective_to, :status, :created_at, :created_at
                )
                """, turnoverRentRuleParameters(rule));
        return rule;
    }

    private MapSqlParameterSource turnoverRentRuleParameters(TurnoverRentRuleRecord rule) {
        return new MapSqlParameterSource()
                .addValue("id", rule.id())
                .addValue("contract_id", rule.contractId())
                .addValue("merchant_id", rule.merchantId())
                .addValue("rule_name", rule.ruleName())
                .addValue("rate_bps", rule.rateBps())
                .addValue("min_amount_cent", rule.minAmountCent())
                .addValue("max_amount_cent", rule.maxAmountCent())
                .addValue("effective_from", rule.effectiveFrom())
                .addValue("effective_to", rule.effectiveTo())
                .addValue("status", rule.status())
                .addValue("created_at", timestampWithTimeZone(rule.createdAt()));
    }

    @Override
    public TurnoverRentRuleTierRecord saveTurnoverRentRuleTier(TurnoverRentRuleTierRecord tier) {
        jdbcTemplate.update("""
                INSERT INTO turnover_rent_rule_tiers (id, rule_id, seq, upper_bound_cent, rate_bps)
                VALUES (:id, :rule_id, :seq, :upper_bound_cent, :rate_bps)
                """, new MapSqlParameterSource()
                .addValue("id", tier.id())
                .addValue("rule_id", tier.ruleId())
                .addValue("seq", tier.seq())
                .addValue("upper_bound_cent", tier.upperBoundCent())
                .addValue("rate_bps", tier.rateBps()));
        return tier;
    }

    @Override
    public List<TurnoverRentRuleTierRecord> listTurnoverRentRuleTiers(String ruleId) {
        return jdbcTemplate.query("""
                SELECT id, rule_id, seq, upper_bound_cent, rate_bps
                  FROM turnover_rent_rule_tiers
                 WHERE rule_id = :rule_id
                 ORDER BY seq
                """, Map.of("rule_id", ruleId), (rs, n) -> new TurnoverRentRuleTierRecord(
                rs.getString("id"), rs.getString("rule_id"), rs.getInt("seq"),
                nullableLong(rs, "upper_bound_cent"), rs.getInt("rate_bps")));
    }

    private TurnoverRentRuleRecord mapTurnoverRentRule(ResultSet rs, int rowNumber) throws SQLException {
        return new TurnoverRentRuleRecord(
                rs.getString("id"),
                rs.getString("contract_id"),
                rs.getString("merchant_id"),
                rs.getString("rule_name"),
                rs.getInt("rate_bps"),
                nullableLong(rs, "min_amount_cent"),
                nullableLong(rs, "max_amount_cent"),
                rs.getObject("effective_from", java.time.LocalDate.class),
                rs.getObject("effective_to", java.time.LocalDate.class),
                rs.getString("status"),
                nullableInstant(rs, "created_at"));
    }

    @Override
    public List<TurnoverSourceRow> listMerchantReflowTurnoverRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        // V334 起 integration_order_syncs.merchant_id 已落（源A渠道门店映射 / 源B对账文件直落）。
        // 抽成基数口径（2026-07-21 用户拍板）：
        //   ① 含税毛额 —— amount_cent 按第三方回传原样落库（无税额拆分、无优惠/赠送 per-order 明细，
        //      诚实边界：现回传口径即为该渠道结算金额；无折扣明细可再扣，不臆造）；
        //   ② 扣退款 —— 逐笔返回，退款行金额取负。第十八批 U 段第 155 条 155-R3（ENUM:257「冲正金额一律按该笔款项在其原账期实际计费时所适用的
        //      档位与费率原额冲回」）：退款带所退原订单的外部单号（original_external_business_id），同渠道、同业务类型、同商户的已同步原订单回流行
        //      作 originId、originAt，由计费引擎同期从原订单里减、跨期按原订单快照冲正；
        //   仅计已归属该商户（merchant_id = :merchant_id）且 status='synced' 的 [from, to) 经营日窗口行；
        //   merchant_id IS NULL（未归属）行不计入任何商户——只进平台级租售比，绝不静默塞给某商户。
        //   第十八批 Q 段第 138 条 138-R2（BSM:1347「凡已计收过门票综合服务费的部分——包括通票或券核销划归本商户的部分，以及单店门票的票款——
        //   一律不再计入抽成基数」；BSM:1330「餐饮和零售商户的 POS 结果回流到平台」）：只计餐饮、零售回流（order、refund 同限），与日结第三方源、
        //   看板 THIRDPARTY_REVENUE_WHERE 同谓词；渠道售出的门票回流（business_type='ticket'）已本地出票、计入门票，不进抽成基数。
        return jdbcTemplate.query("""
                SELECT o.id, o.sync_type, o.amount_cent, o.created_at, origin.id AS origin_id, origin.created_at AS origin_at
                  FROM integration_order_syncs o
                  LEFT JOIN integration_order_syncs origin
                    ON o.sync_type = 'refund'
                   AND origin.provider_code = o.provider_code
                   AND origin.business_type = o.business_type
                   AND origin.sync_type = 'order'
                   AND origin.external_business_id = o.original_external_business_id
                   AND origin.merchant_id = o.merchant_id
                   AND origin.status = 'synced'
                 WHERE o.merchant_id = :merchant_id
                   AND o.status = 'synced'
                   AND o.sync_type IN ('order', 'refund')
                   AND o.business_type IN ('catering', 'retail')
                   AND o.created_at >= :from_inclusive
                   AND o.created_at < :to_exclusive
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("from_inclusive", timestampWithTimeZone(fromInclusive))
                .addValue("to_exclusive", timestampWithTimeZone(toExclusive)), (rs, n) -> "refund".equals(rs.getString("sync_type"))
                ? new TurnoverSourceRow("reflow_refund", rs.getString("id"), rs.getTimestamp("created_at").toInstant(), null,
                        Math.negateExact(rs.getLong("amount_cent")), rs.getString("origin_id"), nullableInstant(rs, "origin_at"))
                : new TurnoverSourceRow("reflow_order", rs.getString("id"), rs.getTimestamp("created_at").toInstant(), null,
                        rs.getLong("amount_cent"), null, null));
    }

    @Override
    public List<TurnoverSourceRow> listForfeitedBarDepositRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        // deposit_settlement='forfeited' 只由三条没收路径写（店员按爽约或超出免费时限取消、超时释放、关台未达低消），且必为已付定金。
        return jdbcTemplate.query("""
                SELECT id, deposit_amount_cent, deposit_settled_at
                  FROM bar_reservations
                 WHERE merchant_id = :merchant_id
                   AND deposit_settlement = 'forfeited'
                   AND deposit_settled_at >= :from_inclusive
                   AND deposit_settled_at < :to_exclusive
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("from_inclusive", timestampWithTimeZone(fromInclusive))
                .addValue("to_exclusive", timestampWithTimeZone(toExclusive)), (rs, n) -> new TurnoverSourceRow("bar_deposit_forfeited",
                rs.getString("id"), rs.getTimestamp("deposit_settled_at").toInstant(), null, rs.getLong("deposit_amount_cent"), null, null));
    }

    @Override
    public List<TurnoverSourceRow> listBarPlatformTurnoverRows(String merchantId, Instant fromInclusive, Instant toExclusive,
            LocalDate firstDay, LocalDate dayAfterLast) {
        // 口径同分红酒吧收入（JdbcDividendRepository.sumBarRevenueCent/sumBarRefundCent）：goodsRevenueCent 已是折后实收，不含券抵扣、服务费与小费分项
        // （BSM:1070、BSM:1077），含补低消与储值付款；整单待付清的结清前不计（BSM:1111）；其余来源未定、服务费未月结定格一律不出账
        // （ENUM:257「账单一次出准，不使用预估数」）。
        com.complex.platform.bar.BarRevenueSourceService sources = barRevenueSources.getObject();
        List<TurnoverSourceRow> rows = new java.util.ArrayList<>();
        for (var row : sources.completedCollections(merchantId, fromInclusive, toExclusive)) {
            if (row.goodsRevenueCent() != null) {
                rows.add(new TurnoverSourceRow("bar_bill", row.billId(), row.completedAt(), null, row.goodsRevenueCent(), null, null));
            } else if (!"payment_pending".equals(row.status())) {
                throw ApiException.conflict("BAR_REVENUE_PENDING", "酒吧收入来源尚待核实（整单分项未齐），暂不能出营业额抽成账单");
            }
        }
        for (var row : sources.withheldServiceFees(merchantId, firstDay, dayAfterLast)) {
            if (row.amountCent() == null) {
                throw ApiException.conflict("BAR_REVENUE_PENDING", "服务费尚未月结定格，暂不能出营业额抽成账单");
            }
            rows.add(new TurnoverSourceRow("bar_withheld_fee", row.sourceBillId() + "|" + row.businessDate(), null, row.businessDate(),
                    row.amountCent(), null, null));
        }
        for (var row : sources.completedGoodsRefunds(merchantId, fromInclusive, toExclusive)) {
            if (row.amountCent() == null) {
                throw ApiException.conflict("BAR_REVENUE_PENDING", "实际退款分项尚待核实，暂不能出营业额抽成账单");
            }
            rows.add(new TurnoverSourceRow("bar_goods_refund", row.adjustmentId(), row.completedAt(), null, Math.negateExact(row.amountCent()),
                    row.billId(), row.billCompletedAt()));
        }
        return rows;
    }

    @Override
    public List<TurnoverSourceRow> listSettledVoucherSettlementRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        // 「已完成结算」取实际付款，按付款时点归期（ENUM:257 账单一次出准、不在下期补差：未付讫的部分到付讫当期再计）。
        // 统一券 internal_price 核销（VoucherService）与通票合作方券 internal_purchased 核销（PartnerVoucherService）都给结算对象挂
        // source_type='partner_voucher' 应付；partner_kind=external_partner 的对手方是外部伙伴 id，按商户 id 过滤自然不计。
        // 第十八批 X 段第 175 条 175-R1（ENUM:257「其通票或券核销划归本商户并已完成结算的部分计入」）：另计通票分账专属直付应付（ticket_distribution）
        // 与补偿履约应付（compensation_fulfillment）的实际付款；两类追收应收（ticket_distribution_recovery、compensation_fulfillment_recovery）的
        // 实际收回＝已完成结算的部分被收回，在收回当期相抵。补偿撤销追加的冲正行与重述行没有付款记录，不重计。
        return jdbcTemplate.query("""
                SELECT p.id, CASE WHEN i.direction = 'receivable' THEN -p.amount_cent ELSE p.amount_cent END AS amount_cent, p.payment_time
                  FROM ar_ap_payments p
                  JOIN ar_ap_items i ON i.id = p.ar_ap_item_id
                 WHERE ((i.direction = 'payable' AND i.source_type IN ('partner_voucher', 'ticket_distribution', 'compensation_fulfillment'))
                        OR (i.direction = 'receivable'
                            AND i.source_type IN ('ticket_distribution_recovery', 'compensation_fulfillment_recovery')))
                   AND i.counterparty_id = :merchant_id
                   AND p.payment_time >= :from_inclusive
                   AND p.payment_time < :to_exclusive
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("from_inclusive", timestampWithTimeZone(fromInclusive))
                .addValue("to_exclusive", timestampWithTimeZone(toExclusive)), (rs, n) -> new TurnoverSourceRow("voucher_settlement",
                rs.getString("id"), rs.getTimestamp("payment_time").toInstant(), null, rs.getLong("amount_cent"), null, null));
    }

    @Override
    public List<TurnoverSourceRow> listCompletedServiceOrderRows(String merchantId, Instant fromInclusive, Instant toExclusive) {
        // BSM:1122「全额进公司账……分给服务人员的部分，是入账之后按薪资/提成/成本名录列支的支出」：计售价 price_cent 全额，不扣服务提供人份额；
        // 只计已完成的单（ENUM:290 服务完成后不可退），已取消的已原路退款、不计；created_at 即下单时点（BSM:1347「按开台或下单时点所属的营业日归属账期」）。
        return jdbcTemplate.query("""
                SELECT id, price_cent, created_at
                  FROM service_orders
                 WHERE bar_id = :merchant_id
                   AND status = 'completed'
                   AND created_at >= :from_inclusive
                   AND created_at < :to_exclusive
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("from_inclusive", timestampWithTimeZone(fromInclusive))
                .addValue("to_exclusive", timestampWithTimeZone(toExclusive)), (rs, n) -> new TurnoverSourceRow("service_order",
                rs.getString("id"), rs.getTimestamp("created_at").toInstant(), null, rs.getLong("price_cent"), null, null));
    }

    @Override
    public Optional<BilledTurnoverSale> findTurnoverSaleSnapshot(String kind, String id) {
        return jdbcTemplate.query("""
                SELECT net_amount_cent, segments::text AS segments, billed_by_minimum
                  FROM turnover_rent_sale_snapshots
                 WHERE source_kind = :source_kind AND source_id = :source_id
                """, new MapSqlParameterSource().addValue("source_kind", kind).addValue("source_id", id),
                (rs, n) -> new BilledTurnoverSale(new TurnoverSaleSnapshot(kind, id, rs.getLong("net_amount_cent"),
                        readSegments(rs.getString("segments"))), rs.getBoolean("billed_by_minimum"))).stream().findFirst();
    }

    @Override
    public void insertTurnoverSaleSnapshots(String merchantId, String period, List<TurnoverSaleSnapshot> snapshots,
            boolean billedByMinimum) {
        for (TurnoverSaleSnapshot snapshot : snapshots) {
            jdbcTemplate.update("""
                    INSERT INTO turnover_rent_sale_snapshots
                        (source_kind, source_id, merchant_id, period, net_amount_cent, segments, billed_by_minimum)
                    VALUES (:source_kind, :source_id, :merchant_id, :period, :net_amount_cent, CAST(:segments AS jsonb), :billed_by_minimum)
                    ON CONFLICT DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("source_kind", snapshot.kind())
                    .addValue("source_id", snapshot.id())
                    .addValue("merchant_id", merchantId)
                    .addValue("period", period)
                    .addValue("net_amount_cent", snapshot.netAmountCent())
                    .addValue("segments", writeJson(snapshot.segments().stream()
                            .map(segment -> Map.of("rate_bps", segment.rateBps(), "amount_cent", segment.amountCent())).toList()))
                    .addValue("billed_by_minimum", billedByMinimum));
        }
    }

    private List<TurnoverRentMath.Segment> readSegments(String json) {
        try {
            List<TurnoverRentMath.Segment> segments = new java.util.ArrayList<>();
            for (var node : objectMapper.readTree(json)) {
                segments.add(new TurnoverRentMath.Segment(node.path("rate_bps").asInt(), node.path("amount_cent").asLong()));
            }
            return segments;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("营业额抽成快照无法读取", e);
        }
    }

    @Override
    public DepositLedgerRecord saveDepositLedger(DepositLedgerRecord ledger) {
        jdbcTemplate.update("""
                INSERT INTO deposit_ledger (
                    id, merchant_id, action, amount_cent, balance_after_cent,
                    ar_ap_item_id, reason, created_at
                )
                VALUES (
                    :id, :merchant_id, :action, :amount_cent, :balance_after_cent,
                    :ar_ap_item_id, :reason, :created_at
                )
                ON CONFLICT (id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", ledger.id())
                .addValue("merchant_id", ledger.merchantId())
                .addValue("action", ledger.action())
                .addValue("amount_cent", ledger.amountCent())
                .addValue("balance_after_cent", ledger.balanceAfterCent())
                .addValue("ar_ap_item_id", ledger.arApItemId())
                .addValue("reason", ledger.reason())
                .addValue("created_at", timestampWithTimeZone(ledger.createdAt())));
        return ledger;
    }

    @Override
    public long currentDepositBalance(String merchantId) {
        Long balance = jdbcTemplate.queryForObject("""
                SELECT COALESCE(
                         (SELECT balance_after_cent
                            FROM deposit_ledger
                           WHERE merchant_id = :merchant_id
                           ORDER BY created_at DESC, id DESC
                           LIMIT 1), 0)
                """, Map.of("merchant_id", merchantId), Long.class);
        return balance == null ? 0L : balance;
    }

    @Override
    public long currentDepositBalanceForUpdate(String merchantId) {
        // P1 修复：对该商户最新一条保证金台账行加 FOR UPDATE 行锁，使并发 collect/deduct 在锁内串行化读取权威余额。
        // 子查询定位最新行后，外层对该行 SELECT ... FOR UPDATE 持锁直至事务结束，杜绝余额读取丢更新与超额校验绕过。
        // 无任何台账时（首笔 collect）无行可锁，退化为 0，由 saveDepositLedger 的插入与后续读锁保证后续动作串行。
        Long locked = jdbcTemplate.query("""
                SELECT balance_after_cent
                  FROM deposit_ledger
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id DESC
                 LIMIT 1
                   FOR UPDATE
                """, Map.of("merchant_id", merchantId),
                (rs, rowNum) -> rs.getLong("balance_after_cent")).stream().findFirst().orElse(null);
        return locked == null ? 0L : locked;
    }

    // ── F-OPS-08 物业费固定计费（V150，替代已废止 CAM）────────────────────────────

    @Override
    public PropertyFeeRateRecord savePropertyFeeRate(PropertyFeeRateRecord rate) {
        jdbcTemplate.update("""
                INSERT INTO property_fee_rates (
                    id, project_id, effective_period, rate_per_square_meter_cent, status, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :effective_period, :rate, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    rate_per_square_meter_cent = EXCLUDED.rate_per_square_meter_cent,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, new MapSqlParameterSource()
                .addValue("id", rate.id())
                .addValue("project_id", rate.projectId())
                .addValue("effective_period", rate.effectivePeriod())
                .addValue("rate", rate.ratePerSquareMeterCent())
                .addValue("status", rate.status())
                .addValue("created_at", timestampWithTimeZone(rate.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(rate.updatedAt())));
        return rate;
    }

    @Override
    public Optional<PropertyFeeRateRecord> findEffectivePropertyFeeRate(String projectId, String period) {
        // 精确 period 命中优先（CASE 排序 0），否则回退默认费率（effective_period IS NULL，排序 1）。
        return jdbcTemplate.query("""
                SELECT id, project_id, effective_period, rate_per_square_meter_cent, status, created_at, updated_at
                  FROM property_fee_rates
                 WHERE project_id = :project_id
                   AND status = 'active'
                   AND (effective_period = :period OR effective_period IS NULL)
                 ORDER BY CASE WHEN effective_period = :period THEN 0 ELSE 1 END
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period", period),
                this::mapPropertyFeeRate).stream().findFirst();
    }

    @Override
    public List<PropertyFeeRateRecord> listPropertyFeeRates(String projectId) {
        return jdbcTemplate.query("""
                SELECT id, project_id, effective_period, rate_per_square_meter_cent, status, created_at, updated_at
                  FROM property_fee_rates
                 WHERE project_id = :project_id
                 ORDER BY effective_period NULLS FIRST
                """, new MapSqlParameterSource().addValue("project_id", projectId),
                this::mapPropertyFeeRate);
    }

    @Override
    public BigDecimal findMerchantAreaSquareMeter(String merchantId) {
        // 只读跨域：finance 不写 merchants，仅取面积用于物业费计费（与 V110 只读 integration_order_syncs 同范式）。
        return jdbcTemplate.query("""
                SELECT area_square_meter FROM merchants WHERE id = :id
                """, new MapSqlParameterSource().addValue("id", merchantId),
                (rs, rowNum) -> rs.getBigDecimal("area_square_meter")).stream().findFirst().orElse(null);
    }

    @Override
    public boolean propertyFeeBillExists(String projectId, String period, String merchantId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM property_fee_bills
                 WHERE project_id = :project_id AND period = :period AND merchant_id = :merchant_id
                """, new MapSqlParameterSource()
                .addValue("project_id", projectId)
                .addValue("period", period)
                .addValue("merchant_id", merchantId), Long.class);
        return count != null && count > 0;
    }

    @Override
    public PropertyFeeBillRecord savePropertyFeeBill(PropertyFeeBillRecord bill) {
        jdbcTemplate.update("""
                INSERT INTO property_fee_bills (
                    id, project_id, period, merchant_id, area_square_meter,
                    rate_per_square_meter_cent, amount_cent, ar_ap_item_id, created_at
                )
                VALUES (
                    :id, :project_id, :period, :merchant_id, :area,
                    :rate, :amount, :ar_ap_item_id, :created_at
                )
                ON CONFLICT (project_id, period, merchant_id) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", bill.id())
                .addValue("project_id", bill.projectId())
                .addValue("period", bill.period())
                .addValue("merchant_id", bill.merchantId())
                .addValue("area", bill.areaSquareMeter())
                .addValue("rate", bill.ratePerSquareMeterCent())
                .addValue("amount", bill.amountCent())
                .addValue("ar_ap_item_id", bill.arApItemId())
                .addValue("created_at", timestampWithTimeZone(bill.createdAt())));
        return bill;
    }

    private PropertyFeeRateRecord mapPropertyFeeRate(ResultSet rs, int rowNum) throws SQLException {
        return new PropertyFeeRateRecord(
                rs.getString("id"),
                rs.getString("project_id"),
                rs.getString("effective_period"),
                rs.getLong("rate_per_square_meter_cent"),
                rs.getString("status"),
                nullableInstant(rs, "created_at"),
                nullableInstant(rs, "updated_at"));
    }

    private DailyCloseSourceTotals aggregateTotals(
            String sql, LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("from", timestampWithTimeZone(fromInclusive))
                .addValue("to", timestampWithTimeZone(toExclusive));
        return jdbcTemplate.query(sql, params, (rs) -> {
            if (!rs.next()) {
                return DailyCloseSourceTotals.empty();
            }
            return new DailyCloseSourceTotals(
                    rs.getLong("amount_cent"),
                    rs.getLong("refund_cent"),
                    rs.getInt("order_count"));
        });
    }

    @Override
    public ArApAdjustmentRecord saveArApAdjustment(ArApAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_adjustments (
                    id, ar_ap_item_id, locked_day_close_id, adjustment_amount_cent,
                    approval_status, requested_by, approved_by, reason, created_at, decided_at
                )
                VALUES (
                    :id, :ar_ap_item_id, :locked_day_close_id, :adjustment_amount_cent,
                    :approval_status, :requested_by, :approved_by, :reason, :created_at, :decided_at
                )
                ON CONFLICT (id) DO NOTHING
                """, adjustmentParameters(adjustment));
        return adjustment;
    }

    /** 第十八批 R 段 143-R4：补录记录连同台账单号与原营业日（锁定该台账的日结的营业日）一并取。 */
    private static final String ADJUSTMENT_SELECT = """
            SELECT a.id, a.ar_ap_item_id, a.locked_day_close_id, a.adjustment_amount_cent,
                   a.approval_status, a.requested_by, a.approved_by, a.reason, a.created_at, a.decided_at,
                   (SELECT i.item_no FROM ar_ap_items i WHERE i.id = a.ar_ap_item_id) AS item_no,
                   (SELECT d.business_date FROM finance_daily_closes d WHERE d.id = a.locked_day_close_id) AS original_business_date
              FROM ar_ap_adjustments a
            """;

    @Override
    public Optional<ArApAdjustmentRecord> findArApAdjustmentByIdForUpdate(String id) {
        return jdbcTemplate.query(ADJUSTMENT_SELECT + """
                 WHERE a.id = :id
                   FOR UPDATE OF a
                """, Map.of("id", id), this::mapAdjustment).stream().findFirst();
    }

    @Override
    public List<ArApAdjustmentRecord> listAdjustmentsByDailyClose(String dailyCloseId) {
        return jdbcTemplate.query(ADJUSTMENT_SELECT + """
                 WHERE a.locked_day_close_id = :daily_close_id
                 ORDER BY a.created_at DESC, a.id
                """, Map.of("daily_close_id", dailyCloseId), this::mapAdjustment);
    }

    @Override
    public ArApAdjustmentRecord updateArApAdjustment(ArApAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                UPDATE ar_ap_adjustments
                   SET approval_status = :approval_status,
                       approved_by = :approved_by,
                       decided_at = :decided_at
                 WHERE id = :id
                """, adjustmentParameters(adjustment));
        return adjustment;
    }

    private MapSqlParameterSource adjustmentParameters(ArApAdjustmentRecord adjustment) {
        return new MapSqlParameterSource()
                .addValue("id", adjustment.id())
                .addValue("ar_ap_item_id", adjustment.arApItemId())
                .addValue("locked_day_close_id", adjustment.lockedDayCloseId())
                .addValue("adjustment_amount_cent", adjustment.adjustmentAmountCent())
                .addValue("approval_status", adjustment.approvalStatus())
                .addValue("requested_by", adjustment.requestedBy())
                .addValue("approved_by", adjustment.approvedBy())
                .addValue("reason", adjustment.reason())
                .addValue("created_at", timestampWithTimeZone(adjustment.createdAt()))
                .addValue("decided_at", timestampWithTimeZone(adjustment.decidedAt()));
    }

    private DailyCloseRecord mapDailyClose(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DailyCloseRecord(
                resultSet.getString("id"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                resultSet.getString("scope_type"),
                resultSet.getString("scope_id"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "locked_at"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "confirmed_at"),
                nullableInstant(resultSet, "archived_at"),
                finalMarkCategories(resultSet.getString("final_mark_categories")));
    }

    private static List<String> finalMarkCategories(String joined) {
        return joined == null ? List.of() : List.of(joined.split(","));
    }

    private DailyCloseSummaryRecord mapDailyCloseSummary(ResultSet resultSet, int rowNumber) throws SQLException {
        return new DailyCloseSummaryRecord(
                resultSet.getString("id"),
                resultSet.getString("daily_close_id"),
                resultSet.getString("source_stream"),
                resultSet.getLong("amount_cent"),
                resultSet.getLong("refund_cent"),
                resultSet.getInt("order_count"),
                nullableInstant(resultSet, "created_at"));
    }

    private ArApAdjustmentRecord mapAdjustment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ArApAdjustmentRecord(
                resultSet.getString("id"),
                resultSet.getString("ar_ap_item_id"),
                resultSet.getString("locked_day_close_id"),
                resultSet.getLong("adjustment_amount_cent"),
                resultSet.getString("approval_status"),
                resultSet.getString("requested_by"),
                resultSet.getString("approved_by"),
                resultSet.getString("reason"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "decided_at"),
                resultSet.getString("item_no"),
                resultSet.getObject("original_business_date", java.time.LocalDate.class));
    }

    private MapSqlParameterSource arApItemParameters(ArApItemRecord item) {
        return new MapSqlParameterSource()
                .addValue("id", item.id())
                .addValue("item_no", item.itemNo())
                .addValue("direction", item.direction())
                .addValue("counterparty_id", item.counterpartyId())
                .addValue("source_type", item.sourceType())
                .addValue("source_id", item.sourceId())
                .addValue("amount_cent", item.amountCent())
                .addValue("paid_amount_cent", item.paidAmountCent())
                .addValue("unpaid_amount_cent", item.unpaidAmountCent())
                .addValue("currency", item.currency())
                .addValue("due_date", item.dueDate())
                .addValue("accounting_period", item.accountingPeriod())
                .addValue("status", item.status())
                .addValue("created_at", timestampWithTimeZone(item.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(item.updatedAt()))
                .addValue("merchant_id", item.merchantId())
                .addValue("project_id", item.projectId())
                .addValue("origin_item_id", item.originItemId())
                .addValue("adjustment_id", item.adjustmentId());
    }

    private MapSqlParameterSource arApItemSourceParameters(ArApItemSourceRecord source) {
        return new MapSqlParameterSource()
                .addValue("id", source.id())
                .addValue("ar_ap_item_id", source.arApItemId())
                .addValue("source_type", source.sourceType())
                .addValue("source_id", source.sourceId())
                .addValue("amount_cent", source.amountCent())
                .addValue("created_at", timestampWithTimeZone(source.createdAt()));
    }

    private MapSqlParameterSource arApPaymentParameters(ArApPaymentRecord payment) {
        return new MapSqlParameterSource()
                .addValue("id", payment.id())
                .addValue("ar_ap_item_id", payment.arApItemId())
                .addValue("amount_cent", payment.amountCent())
                .addValue("payment_method", payment.paymentMethod())
                .addValue("payment_time", timestampWithTimeZone(payment.paymentTime()))
                .addValue("bank_flow_ref", payment.bankFlowRef())
                .addValue("created_at", timestampWithTimeZone(payment.createdAt()));
    }

    private MapSqlParameterSource dailyCloseParameters(DailyCloseRecord dailyClose) {
        return new MapSqlParameterSource()
                .addValue("id", dailyClose.id())
                .addValue("business_date", dailyClose.businessDate())
                .addValue("scope_type", dailyClose.scopeType())
                .addValue("scope_id", dailyClose.scopeId())
                .addValue("status", dailyClose.status())
                .addValue("locked_at", timestampWithTimeZone(dailyClose.lockedAt()))
                .addValue("created_at", timestampWithTimeZone(dailyClose.createdAt()))
                .addValue("confirmed_at", timestampWithTimeZone(dailyClose.confirmedAt()))
                .addValue("archived_at", timestampWithTimeZone(dailyClose.archivedAt()));
    }

    private MapSqlParameterSource paymentIntentParameters(PaymentIntentRecord paymentIntent) {
        return new MapSqlParameterSource()
                .addValue("id", paymentIntent.id())
                .addValue("business_order_type", paymentIntent.businessOrderType())
                .addValue("business_order_id", paymentIntent.businessOrderId())
                .addValue("amount_cent", paymentIntent.amountCent())
                .addValue("currency", paymentIntent.currency())
                .addValue("channel", paymentIntent.channel())
                .addValue("status", paymentIntent.status())
                .addValue("gateway_provider", paymentIntent.gatewayProvider())
                .addValue("gateway_ref", paymentIntent.gatewayRef())
                .addValue("gateway_status", paymentIntent.gatewayStatus())
                .addValue("created_at", timestampWithTimeZone(paymentIntent.createdAt()))
                .addValue("updated_at", timestampWithTimeZone(paymentIntent.updatedAt()));
    }

    private MapSqlParameterSource offlineConfirmationParameters(OfflinePaymentConfirmationRecord confirmation) {
        return new MapSqlParameterSource()
                .addValue("id", confirmation.id())
                .addValue("payment_id", confirmation.paymentId())
                .addValue("amount_cent", confirmation.amountCent())
                .addValue("payment_method", confirmation.paymentMethod())
                .addValue("confirmed_at", timestampWithTimeZone(confirmation.confirmedAt()))
                .addValue("evidence_attachment_ids", writeJson(confirmation.evidenceAttachmentIds()))
                .addValue("status", confirmation.status())
                .addValue("created_at", timestampWithTimeZone(confirmation.createdAt()));
    }

    private MapSqlParameterSource refundParameters(FinanceRefundRecord refund) {
        return new MapSqlParameterSource()
                .addValue("id", refund.id())
                .addValue("target_id", refund.targetId())
                .addValue("refund_amount_cent", refund.refundAmountCent())
                .addValue("reason", refund.reason())
                .addValue("refund_channel", refund.refundChannel())
                .addValue("refund_direction", refund.refundDirection())
                .addValue("status", refund.status())
                .addValue("created_at", timestampWithTimeZone(refund.createdAt()))
                .addValue("source_type", refund.sourceType())
                .addValue("merchant_id", refund.merchantId()).addValue("project_id", refund.projectId())
                .addValue("requested_by", refund.requestedBy())
                .addValue("original_payment_ref", refund.originalPaymentRef())
                .addValue("gateway_provider", refund.gatewayProvider()).addValue("gateway_ref", refund.gatewayRef())
                .addValue("updated_at", timestampWithTimeZone(refund.updatedAt()))
                .addValue("succeeded_at", timestampWithTimeZone(refund.succeededAt()));
    }

    private MapSqlParameterSource reconciliationBatchParameters(ReconciliationBatchRecord batch) {
        return new MapSqlParameterSource()
                .addValue("id", batch.id())
                .addValue("batch_no", batch.batchNo())
                .addValue("scope_type", batch.scopeType())
                .addValue("scope_id", batch.scopeId())
                .addValue("business_date", batch.businessDate())
                .addValue("status", batch.status())
                .addValue("created_at", timestampWithTimeZone(batch.createdAt()));
    }

    private MapSqlParameterSource reconciliationDifferenceParameters(ReconciliationDifferenceRecord difference) {
        return new MapSqlParameterSource()
                .addValue("id", difference.id())
                .addValue("batch_id", difference.batchId())
                .addValue("difference_type", difference.differenceType())
                .addValue("business_ref", difference.businessRef())
                .addValue("amount_cent", difference.amountCent())
                .addValue("expected_amount_cent", difference.expectedAmountCent())
                .addValue("actual_amount_cent", difference.actualAmountCent())
                .addValue("source", difference.source())
                .addValue("status", difference.status())
                .addValue("created_at", timestampWithTimeZone(difference.createdAt()))
                .addValue("evidence_attachment_ids", writeJson(difference.evidenceAttachmentIds()))
                .addValue("responsible_user_id", difference.responsibleUserId())
                .addValue("stage_entered_at", timestampWithTimeZone(difference.stageEnteredAt()))
                .addValue("identified_cause", difference.identifiedCause())
                .addValue("review_id", difference.reviewId());
    }

    private ArApItemRecord mapArApItem(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ArApItemRecord(
                resultSet.getString("id"),
                resultSet.getString("item_no"),
                resultSet.getString("direction"),
                resultSet.getString("counterparty_id"),
                resultSet.getString("source_type"),
                resultSet.getString("source_id"),
                resultSet.getLong("amount_cent"),
                resultSet.getLong("paid_amount_cent"),
                resultSet.getLong("unpaid_amount_cent"),
                resultSet.getString("currency"),
                resultSet.getObject("due_date", java.time.LocalDate.class),
                resultSet.getString("accounting_period"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                nullableInstant(resultSet, "updated_at"),
                resultSet.getString("merchant_id"),
                resultSet.getString("project_id"),
                resultSet.getString("origin_item_id"),
                resultSet.getString("adjustment_id")
        );
    }

    private ArApPaymentRecord mapArApPayment(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ArApPaymentRecord(
                resultSet.getString("id"),
                resultSet.getString("ar_ap_item_id"),
                resultSet.getLong("amount_cent"),
                resultSet.getString("payment_method"),
                nullableInstant(resultSet, "payment_time"),
                resultSet.getString("bank_flow_ref"),
                nullableInstant(resultSet, "created_at")
        );
    }

    private ReconciliationBatchRecord mapReconciliationBatch(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ReconciliationBatchRecord(
                resultSet.getString("id"),
                resultSet.getString("batch_no"),
                resultSet.getString("scope_type"),
                resultSet.getString("scope_id"),
                resultSet.getObject("business_date", java.time.LocalDate.class),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at")
        );
    }

    private ReconciliationDifferenceRecord mapReconciliationDifference(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ReconciliationDifferenceRecord(
                resultSet.getString("id"),
                resultSet.getString("batch_id"),
                resultSet.getString("difference_type"),
                resultSet.getString("business_ref"),
                resultSet.getLong("amount_cent"),
                nullableLong(resultSet, "expected_amount_cent"),
                nullableLong(resultSet, "actual_amount_cent"),
                resultSet.getString("source"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at"),
                readStringList(resultSet.getString("evidence_attachment_ids")),
                resultSet.getString("responsible_user_id"),
                nullableInstant(resultSet, "stage_entered_at"),
                resultSet.getString("identified_cause"),
                resultSet.getString("review_id"),
                nullableInstant(resultSet, "stage_escalated_at")
        );
    }

    private Long nullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }



    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("退款凭证记录无法读取", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("finance JSON value is not serializable", exception);
        }
    }
}
