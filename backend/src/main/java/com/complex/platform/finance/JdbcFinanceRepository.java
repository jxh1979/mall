package com.complex.platform.finance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
                    accounting_period, status, created_at, updated_at
                )
                VALUES (
                    :id, :item_no, :direction, :counterparty_id, :source_type, :source_id,
                    :amount_cent, :paid_amount_cent, :unpaid_amount_cent, :currency, :due_date,
                    :accounting_period, :status, :created_at, :updated_at
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
                       accounting_period, status, created_at, updated_at
                  FROM ar_ap_items
                 ORDER BY created_at DESC, id
                """, this::mapArApItem);
    }

    @Override
    public Optional<ArApItemRecord> findArApItemById(String id) {
        return jdbcTemplate.query("""
                SELECT id, item_no, direction, counterparty_id, source_type, source_id,
                       amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date,
                       accounting_period, status, created_at, updated_at
                  FROM ar_ap_items
                 WHERE id = :id
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
        jdbcTemplate.update("""
                INSERT INTO ar_ap_payments (
                    id, ar_ap_item_id, amount_cent, payment_method, payment_time,
                    bank_flow_ref, created_at
                )
                VALUES (
                    :id, :ar_ap_item_id, :amount_cent, :payment_method, :payment_time,
                    :bank_flow_ref, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id,
                    amount_cent = EXCLUDED.amount_cent,
                    payment_method = EXCLUDED.payment_method,
                    payment_time = EXCLUDED.payment_time,
                    bank_flow_ref = EXCLUDED.bank_flow_ref
                """, arApPaymentParameters(payment));
        return payment;
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
                .addValue("updated_at", timestampWithTimeZone(item.updatedAt()));
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
                nullableInstant(resultSet, "updated_at")
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

    private OffsetDateTime timestampWithTimeZone(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    private Instant nullableInstant(ResultSet resultSet, String columnName) throws SQLException {
        OffsetDateTime value = resultSet.getObject(columnName, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
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
