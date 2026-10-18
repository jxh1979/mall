package com.complex.platform.finance;

// 文件版本：V0.02
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
        int safePage = page < 1 ? 1 : page;
        int safeSize = size < 1 ? 20 : Math.min(size, 200);
        String trimmed = keyword == null ? "" : keyword.trim();
        // P1-A 两维并存:merchant_id(商户维) + project_id(综合体维);非空才加谓词,均空=平台全量。
        // 参数带 SQL 类型,PG 才能推断 :x IS NULL 分支的类型。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR)
                .addValue("project_id", projectId, java.sql.Types.VARCHAR)
                .addValue("kw", trimmed.isEmpty() ? null : "%" + trimmed + "%", java.sql.Types.VARCHAR);
        String where = """
                 WHERE (:merchant_id IS NULL OR merchant_id = :merchant_id)
                   AND (:project_id IS NULL OR project_id = :project_id)
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
                    id, business_date, scope_type, scope_id, force_close, status, locked_at, created_at
                )
                VALUES (
                    :id, :business_date, :scope_type, :scope_id, :force_close, :status, :locked_at, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_date = EXCLUDED.business_date,
                    scope_type = EXCLUDED.scope_type,
                    scope_id = EXCLUDED.scope_id,
                    force_close = EXCLUDED.force_close,
                    status = EXCLUDED.status,
                    locked_at = EXCLUDED.locked_at
                """, dailyCloseParameters(dailyClose));
        return dailyClose;
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
    public List<ReconciliationBatchRecord> listReconciliationBatches() {
        return jdbcTemplate.query("""
                SELECT id, batch_no, scope_type, scope_id, business_date, status, created_at
                  FROM finance_reconciliation_batches
                 ORDER BY created_at DESC, id
                """, this::mapReconciliationBatch);
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
    public List<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return jdbcTemplate.query("""
                SELECT id, batch_id, difference_type, business_ref, amount_cent,
                       expected_amount_cent, actual_amount_cent, source, status, created_at
                  FROM finance_reconciliation_differences
                 ORDER BY created_at DESC, id
                """, this::mapReconciliationDifference);
    }

    @Override
    public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference) {
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_differences (
                    id, batch_id, difference_type, business_ref, amount_cent,
                    expected_amount_cent, actual_amount_cent, source, status, created_at
                )
                VALUES (
                    :id, :batch_id, :difference_type, :business_ref, :amount_cent,
                    :expected_amount_cent, :actual_amount_cent, :source, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    batch_id = EXCLUDED.batch_id,
                    difference_type = EXCLUDED.difference_type,
                    business_ref = EXCLUDED.business_ref,
                    amount_cent = EXCLUDED.amount_cent,
                    expected_amount_cent = EXCLUDED.expected_amount_cent,
                    actual_amount_cent = EXCLUDED.actual_amount_cent,
                    source = EXCLUDED.source,
                    status = EXCLUDED.status
                """, reconciliationDifferenceParameters(difference));
        return difference;
    }

    @Override
    public Optional<ReconciliationDifferenceRecord> findReconciliationDifferenceById(String id) {
        return jdbcTemplate.query("""
                SELECT id, batch_id, difference_type, business_ref, amount_cent,
                       expected_amount_cent, actual_amount_cent, source, status, created_at
                  FROM finance_reconciliation_differences
                 WHERE id = :id
                """, Map.of("id", id), this::mapReconciliationDifference).stream().findFirst();
    }

    @Override
    public ReconciliationDifferenceRecord closeReconciliationDifference(
            String id, String closeReason, String closedBy, java.time.Instant closedAt) {
        jdbcTemplate.update("""
                UPDATE finance_reconciliation_differences
                   SET status = 'closed', close_reason = :close_reason,
                       closed_by = :closed_by, closed_at = :closed_at
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("close_reason", closeReason)
                .addValue("closed_by", closedBy)
                .addValue("closed_at", timestampWithTimeZone(closedAt)));
        return findReconciliationDifferenceById(id)
                .orElseThrow(() -> new IllegalStateException("reconciliation difference vanished after close: " + id));
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
        Long total = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_cent), 0)
                  FROM ar_ap_payments
                 WHERE id = :business_ref
                """, Map.of("business_ref", businessRef), Long.class);
        return total == null ? 0L : total;
    }

    // ── R5 T7：日结锁定 + 补录审批 + 多源汇总 ──────────────────────────────

    @Override
    public Optional<DailyCloseRecord> findDailyCloseById(String id) {
        return jdbcTemplate.query("""
                SELECT id, business_date, scope_type, scope_id, force_close, status, locked_at, created_at
                  FROM finance_daily_closes
                 WHERE id = :id
                """, Map.of("id", id), this::mapDailyClose).stream().findFirst();
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
        // F-OPS-09：商户级日结锁。与 lockArApItemsForDailyClose 同口径——
        // merchantId 非空（商户 scope 调用方）：仅本商户封账（scope_type='merchant' AND scope_id=:merchant_id）拦截，
        //   他商户封账不冻结本商户写入；
        // merchantId 为空（平台口径调用方）：该经营日任一 locked 日结即拦截（平台口径写入受所有封账约束）。
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                // merchantId 可为 null(平台口径):必须带 SQL 类型,否则 PG 无法推断 :merchant_id IS NULL 的参数类型直接 500。
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM finance_daily_closes
                 WHERE business_date = :business_date
                   AND status = 'locked'
                   AND (:merchant_id IS NULL
                        OR (scope_type = 'merchant' AND scope_id = :merchant_id))
                """, params, Integer.class);
        return count != null && count > 0;
    }

    @Override
    public DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 官方售票：ticket_orders 已支付(paid)，以 visit_date 为经营日；金额=实付分，count=单数。
        // 仅归集非渠道售票；成功退款按 succeeded_at 落在经营日窗口 [from, to) 冲减本源收入。
        // 财务确认：revenue.net = 自营售票 + 酒吧 − 退款（不含第三方回流）；退款冲减按经营日发生。
        return aggregateTotals("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel <> 'channel'
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel <> 'channel'
                   AND o.visit_date = :business_date
                """, businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // P1#15：第三方渠道售票接入真实数据源。ticket_orders.sales_channel='channel'（V009 售票渠道枚举中的渠道售票，
        // 区别于自营 admin/kiosk/miniapp/app/web）已支付(paid)，以 visit_date 为经营日；金额=实付分，count=单数。
        // 退款（冲减当日净收入）：ticket_refunds 按渠道售票订单、created_at 落经营日窗口 [from, to) 归集为 refund_cent。
        // 跨域只读：仅读 ticketing 域既有表，不改其写。
        return aggregateTotals("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel = 'channel'
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel = 'channel'
                   AND o.visit_date = :business_date
                """, businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateBar(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 酒吧：bar_bill_settlements 已结算（settle_channel 全计），以 settled_at 落在经营日窗口 [from, to) 归集；
        // 金额=结算分，count=结算笔数。
        // 退款（待确认#2）：bar_bill_adjustments 中 adjustment_type='refund'（结账后退款减项）按 refunded_at
        //   落经营日窗口 [from, to) 合计为 refund_cent，冲减当日 net。
        //   免单 waiver 不计入 refund_cent——免单在结账前已减应付、结算金额本就更低，避免与低结算重复冲减。
        return aggregateTotals("""
                SELECT (SELECT COALESCE(SUM(settlement_amount_cent), 0)
                          FROM bar_bill_settlements
                         WHERE settled_at >= :from
                           AND settled_at < :to
                           AND reversed = FALSE) AS amount_cent,
                       (SELECT COALESCE(SUM(amount_cent), 0)
                          FROM bar_bill_adjustments
                         WHERE adjustment_type = 'refund'
                           AND refunded_at >= :from
                           AND refunded_at < :to) AS refund_cent,
                       (SELECT COUNT(*)
                          FROM bar_bill_settlements
                         WHERE settled_at >= :from
                           AND settled_at < :to
                           AND reversed = FALSE) AS order_count
                """, businessDate, fromInclusive, toExclusive);
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
        // 第三方回流（餐饮/零售）：integration_order_syncs 回流已 synced 的 order 类，以 created_at 落在经营日窗口归集。
        // 财务确认：第三方回流仅作商户结算口径单列展示，不计入平台 revenue.net（净收入派生时剔除本源）。
        return aggregateTotals("""
                SELECT COALESCE(SUM(amount_cent), 0) AS amount_cent,
                       0 AS refund_cent,
                       COUNT(*) AS order_count
                  FROM integration_order_syncs
                 WHERE sync_type = 'order'
                   AND status = 'synced'
                   AND created_at >= :from
                   AND created_at < :to
                """, businessDate, fromInclusive, toExclusive);
    }

    // ── P2-A：六源汇总按 scope 取数(与 lockArApItemsForDailyClose 口径对齐)──────────
    // 口径:仅 scopeType='merchant' 时按 merchant 维切片(源表有 merchant_id 才能归属,无则归零);
    //       其余 scopeType(project/bar/ticketing/amusement)与既有锁定口径一致=平台全量(委托无 scope 版)。
    // 无 merchant 维的源(amusement/supply_cost/thirdparty_reflow)在 merchant scope 下归零,不再渗入全平台。
    private static final DailyCloseSourceTotals ZERO = DailyCloseSourceTotals.empty();

    private boolean isMerchantScope(String scopeType) {
        return "merchant".equals(scopeType);
    }

    private DailyCloseSourceTotals aggregateTotalsScoped(
            String sql, LocalDate businessDate, Instant from, Instant to, String merchantId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("business_date", businessDate)
                .addValue("from", timestampWithTimeZone(from))
                .addValue("to", timestampWithTimeZone(to))
                .addValue("merchant_id", merchantId, java.sql.Types.VARCHAR);
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
        if (!isMerchantScope(scopeType)) {
            return aggregateOfficialTicket(businessDate, from, to);
        }
        // ticket_orders 有 merchant_id:merchant scope 仅归本商户;退款按本商户订单归集。
        return aggregateTotalsScoped("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel <> 'channel'
                           AND ro.merchant_id = :merchant_id
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel <> 'channel'
                   AND o.visit_date = :business_date
                   AND o.merchant_id = :merchant_id
                """, businessDate, from, to, scopeId);
    }

    @Override
    public DailyCloseSourceTotals aggregateChannelTicket(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        if (!isMerchantScope(scopeType)) {
            return aggregateChannelTicket(businessDate, from, to);
        }
        return aggregateTotalsScoped("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                          JOIN ticket_orders ro ON ro.id = r.target_id
                         WHERE ro.sales_channel = 'channel'
                           AND ro.merchant_id = :merchant_id
                           AND r.status = 'success'
                           AND r.succeeded_at >= :from
                           AND r.succeeded_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.sales_channel = 'channel'
                   AND o.visit_date = :business_date
                   AND o.merchant_id = :merchant_id
                """, businessDate, from, to, scopeId);
    }

    @Override
    public DailyCloseSourceTotals aggregateBar(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        if (!isMerchantScope(scopeType)) {
            return aggregateBar(businessDate, from, to);
        }
        // bar_bill_settlements 有 merchant_id:merchant scope 仅归本商户结算;调整(退款)经 bill→settlement 归本商户。
        return aggregateTotalsScoped("""
                SELECT (SELECT COALESCE(SUM(settlement_amount_cent), 0)
                          FROM bar_bill_settlements
                         WHERE settled_at >= :from
                           AND settled_at < :to
                           AND reversed = FALSE
                           AND merchant_id = :merchant_id) AS amount_cent,
                       (SELECT COALESCE(SUM(adj.amount_cent), 0)
                          FROM bar_bill_adjustments adj
                          JOIN bar_bill_settlements bs ON bs.bill_id = adj.bill_id
                         WHERE adj.adjustment_type = 'refund'
                           AND adj.refunded_at >= :from
                           AND adj.refunded_at < :to
                           AND bs.merchant_id = :merchant_id) AS refund_cent,
                       (SELECT COUNT(*)
                          FROM bar_bill_settlements
                         WHERE settled_at >= :from
                           AND settled_at < :to
                           AND reversed = FALSE
                           AND merchant_id = :merchant_id) AS order_count
                """, businessDate, from, to, scopeId);
    }

    @Override
    public DailyCloseSourceTotals aggregateAmusement(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        // amusement_verifications 无 merchant 维:merchant scope 归零(不属于任一商户);其余 scope 平台全量。
        return isMerchantScope(scopeType) ? ZERO : aggregateAmusement(businessDate, from, to);
    }

    @Override
    public DailyCloseSourceTotals aggregateSupplyCost(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        // inventory_stock_movements 无 merchant 维:merchant scope 归零;其余 scope 平台全量。
        return isMerchantScope(scopeType) ? ZERO : aggregateSupplyCost(businessDate, from, to);
    }

    @Override
    public DailyCloseSourceTotals aggregateThirdpartyReflow(
            LocalDate businessDate, Instant from, Instant to, String scopeType, String scopeId) {
        // integration_order_syncs 无 merchant 维:merchant scope 归零;其余 scope 平台全量。
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
    public long aggregateMerchantReflowTurnover(String merchantId, Instant fromInclusive, Instant toExclusive) {
        // V334 起 integration_order_syncs.merchant_id 已落（源A渠道门店映射 / 源B对账文件直落）。
        // 抽成基数口径（2026-07-21 用户拍板）：
        //   ① 含税毛额 —— amount_cent 按第三方回传原样落库（无税额拆分、无优惠/赠送 per-order 明细，
        //      诚实边界：现回传口径即为该渠道结算金额；无折扣明细可再扣，不臆造）；
        //   ② 扣退款 —— 实收 = SUM(order.amount_cent) − SUM(refund.amount_cent)，同商户同窗口相抵；
        //      退款超出订单时钳到 0（负营业额无业务意义，不作为抽成基数）；
        //   仅计已归属该商户（merchant_id = :merchant_id）且 status='synced' 的 [from, to) 经营日窗口行；
        //   merchant_id IS NULL（未归属）行不计入任何商户——只进平台级租售比，绝不静默塞给某商户。
        Long netTurnover = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(
                         CASE sync_type
                             WHEN 'order'  THEN amount_cent
                             WHEN 'refund' THEN -amount_cent
                             ELSE 0
                         END), 0)
                  FROM integration_order_syncs
                 WHERE merchant_id = :merchant_id
                   AND status = 'synced'
                   AND sync_type IN ('order', 'refund')
                   AND created_at >= :from_inclusive
                   AND created_at < :to_exclusive
                """, new MapSqlParameterSource()
                .addValue("merchant_id", merchantId)
                .addValue("from_inclusive", timestampWithTimeZone(fromInclusive))
                .addValue("to_exclusive", timestampWithTimeZone(toExclusive)), Long.class);
        return netTurnover == null ? 0L : Math.max(0L, netTurnover);
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

    @Override
    public Optional<ArApAdjustmentRecord> findArApAdjustmentByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, ar_ap_item_id, locked_day_close_id, adjustment_amount_cent,
                       approval_status, requested_by, approved_by, reason, created_at, decided_at
                  FROM ar_ap_adjustments
                 WHERE id = :id
                   FOR UPDATE
                """, Map.of("id", id), this::mapAdjustment).stream().findFirst();
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
                resultSet.getBoolean("force_close"),
                resultSet.getString("status"),
                nullableInstant(resultSet, "locked_at"),
                nullableInstant(resultSet, "created_at"));
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
                nullableInstant(resultSet, "decided_at"));
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
                .addValue("force_close", dailyClose.forceClose())
                .addValue("status", dailyClose.status())
                .addValue("locked_at", timestampWithTimeZone(dailyClose.lockedAt()))
                .addValue("created_at", timestampWithTimeZone(dailyClose.createdAt()));
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
                .addValue("created_at", timestampWithTimeZone(difference.createdAt()));
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
                nullableInstant(resultSet, "created_at")
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
