package com.complex.platform.finance;
import static com.complex.platform.common.jdbc.JdbcTimestamps.nullableInstant;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

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
                    accounting_period, status, created_at, updated_at, merchant_id
                )
                VALUES (
                    :id, :item_no, :direction, :counterparty_id, :source_type, :source_id,
                    :amount_cent, :paid_amount_cent, :unpaid_amount_cent, :currency, :due_date,
                    :accounting_period, :status, :created_at, :updated_at, :merchant_id
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
        return jdbcTemplate.query("""
                SELECT id, item_no, direction, counterparty_id, source_type, source_id,
                       amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                       accounting_period, status, created_at, updated_at, merchant_id
                  FROM ar_ap_items
                 ORDER BY created_at DESC, id
                """, this::mapArApItem);
    }

    @Override
    public List<ArApItemRecord> listArApItemsByMerchant(String merchantId) {
        return jdbcTemplate.query("""
                SELECT id, item_no, direction, counterparty_id, source_type, source_id,
                       amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                       accounting_period, status, created_at, updated_at, merchant_id
                  FROM ar_ap_items
                 WHERE merchant_id = :merchant_id
                 ORDER BY created_at DESC, id
                """, Map.of("merchant_id", merchantId), this::mapArApItem);
    }

    @Override
    public Optional<ArApItemRecord> findArApItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, item_no, direction, counterparty_id, source_type, source_id,
                       amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                       accounting_period, status, created_at, updated_at, merchant_id
                  FROM ar_ap_items
                 WHERE id = :id
                """, Map.of("id", id), this::mapArApItem).stream().findFirst();
    }

    @Override
    public Optional<ArApItemRecord> findArApItemByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, item_no, direction, counterparty_id, source_type, source_id,
                       amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                       accounting_period, status, created_at, updated_at, merchant_id
                  FROM ar_ap_items
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
    public long sumRefundedAmountByTargetId(String targetId) {
        Long result = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(refund_amount_cent), 0)
                  FROM finance_refunds
                 WHERE target_id = :target_id
                """, Map.of("target_id", targetId), Long.class);
        return result == null ? 0L : result;
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
                    channel, status, created_at, updated_at
                )
                VALUES (
                    :id, :business_order_type, :business_order_id, :amount_cent, :currency,
                    :channel, :status, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    business_order_type = EXCLUDED.business_order_type,
                    business_order_id = EXCLUDED.business_order_id,
                    amount_cent = EXCLUDED.amount_cent,
                    currency = EXCLUDED.currency,
                    channel = EXCLUDED.channel,
                    status = EXCLUDED.status,
                    updated_at = EXCLUDED.updated_at
                """, paymentIntentParameters(paymentIntent));
        return paymentIntent;
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
                    id, target_id, refund_amount_cent, reason, refund_channel, status, created_at
                )
                VALUES (
                    :id, :target_id, :refund_amount_cent, :reason, :refund_channel, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    target_id = EXCLUDED.target_id,
                    refund_amount_cent = EXCLUDED.refund_amount_cent,
                    reason = EXCLUDED.reason,
                    refund_channel = EXCLUDED.refund_channel,
                    status = EXCLUDED.status
                """, refundParameters(refund));
        return refund;
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
                SELECT id, batch_id, difference_type, business_ref, amount_cent, status, created_at
                  FROM finance_reconciliation_differences
                 ORDER BY created_at DESC, id
                """, this::mapReconciliationDifference);
    }

    @Override
    public ReconciliationDifferenceRecord saveReconciliationDifference(ReconciliationDifferenceRecord difference) {
        jdbcTemplate.update("""
                INSERT INTO finance_reconciliation_differences (
                    id, batch_id, difference_type, business_ref, amount_cent, status, created_at
                )
                VALUES (
                    :id, :batch_id, :difference_type, :business_ref, :amount_cent, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    batch_id = EXCLUDED.batch_id,
                    difference_type = EXCLUDED.difference_type,
                    business_ref = EXCLUDED.business_ref,
                    amount_cent = EXCLUDED.amount_cent,
                    status = EXCLUDED.status
                """, reconciliationDifferenceParameters(difference));
        return difference;
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
                .addValue("merchant_id", merchantId);
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
    public boolean lockedDailyCloseExistsForDate(LocalDate businessDate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM finance_daily_closes
                 WHERE business_date = :business_date
                   AND status = 'locked'
                """, Map.of("business_date", businessDate), Integer.class);
        return count != null && count > 0;
    }

    @Override
    public DailyCloseSourceTotals aggregateOfficialTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 官方售票：ticket_orders 已支付(paid)，以 visit_date 为经营日；金额=实付分，count=单数。
        // 退款（冲减当日净收入）：ticket_refunds 以 created_at 落在经营日窗口 [from, to) 归集，单列入本源 refund_cent。
        // 财务确认：revenue.net = 自营售票 + 酒吧 − 退款（不含第三方回流）；退款冲减按经营日发生。
        return aggregateTotals("""
                SELECT COALESCE(SUM(o.amount_cent), 0) AS amount_cent,
                       (SELECT COALESCE(SUM(r.refund_amount_cent), 0)
                          FROM ticket_refunds r
                         WHERE r.created_at >= :from
                           AND r.created_at < :to) AS refund_cent,
                       COUNT(*) AS order_count
                  FROM ticket_orders o
                 WHERE o.pay_status = 'paid'
                   AND o.visit_date = :business_date
                """, businessDate, fromInclusive, toExclusive);
    }

    @Override
    public DailyCloseSourceTotals aggregateChannelTicket(LocalDate businessDate, Instant fromInclusive, Instant toExclusive) {
        // 第三方渠道售票回流：integration_order_syncs 中票务渠道回流。当前 business_type CHECK 仅 catering/retail，
        // 票务回流维度尚未在该表区分（待财务/集成复核），故保守取 0 计数 0 金额，并不阻断锁定。
        return DailyCloseSourceTotals.empty();
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
                           AND settled_at < :to) AS amount_cent,
                       (SELECT COALESCE(SUM(amount_cent), 0)
                          FROM bar_bill_adjustments
                         WHERE adjustment_type = 'refund'
                           AND refunded_at >= :from
                           AND refunded_at < :to) AS refund_cent,
                       (SELECT COUNT(*)
                          FROM bar_bill_settlements
                         WHERE settled_at >= :from
                           AND settled_at < :to) AS order_count
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
                .addValue("merchant_id", item.merchantId());
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
                .addValue("status", refund.status())
                .addValue("created_at", timestampWithTimeZone(refund.createdAt()));
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
                resultSet.getString("merchant_id")
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
                resultSet.getString("status"),
                nullableInstant(resultSet, "created_at")
        );
    }



    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? List.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("finance JSON value is not serializable", exception);
        }
    }

    @SuppressWarnings("unused")
    private List<String> readStringList(String value) {
        try {
            return value == null ? List.of() : objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("finance JSON value is not readable", exception);
        }
    }
}
