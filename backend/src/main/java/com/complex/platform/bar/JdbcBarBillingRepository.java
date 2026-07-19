package com.complex.platform.bar;
import static com.complex.platform.common.jdbc.JdbcTimestamps.timestampWithTimeZone;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

final class JdbcBarBillingRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcBarRowMapper rowMapper;
    private final JdbcBarParameterSource parameterSource;

    JdbcBarBillingRepository(
            NamedParameterJdbcTemplate jdbcTemplate,
            JdbcBarRowMapper rowMapper,
            JdbcBarParameterSource parameterSource
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
        this.parameterSource = parameterSource;
    }

    public BarBillRecord saveBill(BarBillRecord bill) {
        jdbcTemplate.update("""
                INSERT INTO bar_bills (
                    id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                    subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                    payable_amount_cent, paid_amount_cent, currency, payment_status,
                    settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                    reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                    paid_at
                )
                VALUES (
                    :id, :bill_no, :order_id, :bar_id, :merchant_id, :table_id, :reservation_id,
                    :subtotal_amount_cent, :discount_amount_cent, :deposit_deduct_amount_cent,
                    :payable_amount_cent, :paid_amount_cent, :currency, :payment_status,
                    :settlement_status, :sales_staff_id, :sales_role_type, :created_at, :updated_at,
                    :reopened, :reopened_by_user_id, :reopened_at, :reopen_reason, :reopen_count, :tip_amount_cent,
                    :paid_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    merchant_id = EXCLUDED.merchant_id,
                    subtotal_amount_cent = EXCLUDED.subtotal_amount_cent,
                    discount_amount_cent = EXCLUDED.discount_amount_cent,
                    deposit_deduct_amount_cent = EXCLUDED.deposit_deduct_amount_cent,
                    payable_amount_cent = EXCLUDED.payable_amount_cent,
                    paid_amount_cent = EXCLUDED.paid_amount_cent,
                    payment_status = EXCLUDED.payment_status,
                    settlement_status = EXCLUDED.settlement_status,
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    sales_role_type = EXCLUDED.sales_role_type,
                    updated_at = EXCLUDED.updated_at,
                    reopened = EXCLUDED.reopened,
                    reopened_by_user_id = EXCLUDED.reopened_by_user_id,
                    reopened_at = EXCLUDED.reopened_at,
                    reopen_reason = EXCLUDED.reopen_reason,
                    reopen_count = EXCLUDED.reopen_count,
                    tip_amount_cent = EXCLUDED.tip_amount_cent,
                    -- V231：结账成功写入的 paid_at 随 upsert 落库（billWithAmounts 已保证已 paid 不覆盖、反结清空）。
                    paid_at = EXCLUDED.paid_at
                """, parameterSource.billParameters(bill));
        return bill;
    }

    public BarBillSettlementRecord saveBillSettlement(BarBillSettlementRecord settlement) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_settlements (
                    id, bill_id, settle_channel, cashier_staff_id, pos_transaction_no,
                    settlement_amount_cent, merchant_id, settled_at
                )
                VALUES (
                    :id, :bill_id, :settle_channel, :cashier_staff_id, :pos_transaction_no,
                    :settlement_amount_cent, :merchant_id, :settled_at
                )
                ON CONFLICT (id) DO NOTHING
                """, parameterSource.billSettlementParameters(settlement));
        return settlement;
    }

    public List<BarBillSettlementRecord> listBillSettlements(String billId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_bill_settlements WHERE bill_id = :bill_id ORDER BY settled_at, id
                """, Map.of("bill_id", billId), rowMapper::mapBillSettlement);
    }

    public Optional<BarBillRecord> findBillById(String id) {
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                       reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                       paid_at
                  FROM bar_bills
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapBill).stream().findFirst();
    }

    // 2c-2 结算/计提行锁：SELECT ... FOR UPDATE 序列化并发 settle/commission，防 TOCTOU 重复结算/重复计提。
    public Optional<BarBillRecord> findBillByIdForUpdate(String id) {
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                       reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                       paid_at
                  FROM bar_bills
                 WHERE id = :id
                 FOR UPDATE
                """, Map.of("id", id), rowMapper::mapBill).stream().findFirst();
    }

    List<BarBillRecord> listBillsByOrderIds(List<String> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, bill_no, order_id, bar_id, merchant_id, table_id, reservation_id,
                       subtotal_amount_cent, discount_amount_cent, deposit_deduct_amount_cent,
                       payable_amount_cent, paid_amount_cent, currency, payment_status,
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at,
                       reopened, reopened_by_user_id, reopened_at, reopen_reason, reopen_count, tip_amount_cent,
                       paid_at
                  FROM bar_bills
                 WHERE order_id IN (:order_ids)
                 ORDER BY created_at DESC, id
                """, Map.of("order_ids", orderIds), rowMapper::mapBill);
    }

    // 2c-2 提成防重：按 bill_id 查已有提成计算（calculateCommission 查重守卫用，避免同一账单多幂等键重复计提）。
    public Optional<BarCommissionCalculationRecord> findCommissionByBillId(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,
                       commission_rate_bp, commission_amount_cent, status, created_at
                  FROM bar_commission_calculations
                 WHERE bill_id = :bill_id
                 ORDER BY created_at ASC, id
                 LIMIT 1
                """, Map.of("bill_id", billId), rowMapper::mapCommission).stream().findFirst();
    }

    // 2c-3 reopen 冲正：把账单现有结算流水标 reversed（聚合侧排除），避免反结→重结被双计。
    public int markBillSettlementsReversed(String billId) {
        return jdbcTemplate.update("""
                UPDATE bar_bill_settlements
                   SET reversed = TRUE
                 WHERE bill_id = :bill_id
                   AND reversed = FALSE
                """, Map.of("bill_id", billId));
    }

    public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_adjustments (
                    id, bill_id, adjustment_type, amount_cent,
                    before_payable_amount_cent, after_payable_amount_cent,
                    reason, requested_by_user_id, approval_required, status,
                    idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json,
                    created_at, refunded_at
                )
                VALUES (
                    :id, :bill_id, :adjustment_type, :amount_cent,
                    :before_payable_amount_cent, :after_payable_amount_cent,
                    :reason, :requested_by_user_id, :approval_required, :status,
                    :idempotency_key, CAST(:evidence_attachment_ids AS jsonb),
                    :ar_ap_item_id, CAST(:snapshot_json AS jsonb), :created_at, :refunded_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    amount_cent = EXCLUDED.amount_cent,
                    before_payable_amount_cent = EXCLUDED.before_payable_amount_cent,
                    after_payable_amount_cent = EXCLUDED.after_payable_amount_cent,
                    reason = EXCLUDED.reason,
                    requested_by_user_id = EXCLUDED.requested_by_user_id,
                    approval_required = EXCLUDED.approval_required,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    ar_ap_item_id = EXCLUDED.ar_ap_item_id,
                    snapshot_json = EXCLUDED.snapshot_json,
                    refunded_at = EXCLUDED.refunded_at
                """, parameterSource.billAdjustmentParameters(adjustment));
        return adjustment;
    }

    public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, adjustment_type, amount_cent,
                       before_payable_amount_cent, after_payable_amount_cent,
                       reason, requested_by_user_id, approval_required, status,
                       idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json,
                       created_at, refunded_at
                  FROM bar_bill_adjustments
                 WHERE bill_id = :bill_id
                 ORDER BY created_at, id
                """, Map.of("bill_id", billId), rowMapper::mapBillAdjustment);
    }

    public BarBillOperationRecord saveBillOperation(BarBillOperationRecord operation) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_operations (
                    id, operation_type, primary_bill_id, secondary_bill_id, generated_bill_id,
                    amount_cent, reason, requested_by_user_id, status, idempotency_key,
                    evidence_attachment_ids, snapshot_json, created_at
                )
                VALUES (
                    :id, :operation_type, :primary_bill_id, :secondary_bill_id, :generated_bill_id,
                    :amount_cent, :reason, :requested_by_user_id, :status, :idempotency_key,
                    CAST(:evidence_attachment_ids AS jsonb), CAST(:snapshot_json AS jsonb), :created_at
                )
                ON CONFLICT (primary_bill_id, idempotency_key) DO UPDATE SET
                    secondary_bill_id = EXCLUDED.secondary_bill_id,
                    generated_bill_id = EXCLUDED.generated_bill_id,
                    amount_cent = EXCLUDED.amount_cent,
                    reason = EXCLUDED.reason,
                    requested_by_user_id = EXCLUDED.requested_by_user_id,
                    status = EXCLUDED.status,
                    evidence_attachment_ids = EXCLUDED.evidence_attachment_ids,
                    snapshot_json = EXCLUDED.snapshot_json
                """, parameterSource.billOperationParameters(operation));
        return operation;
    }

    public List<BarBillOperationRecord> listBillOperations(String billId) {
        return jdbcTemplate.query("""
                SELECT id, operation_type, primary_bill_id, secondary_bill_id, generated_bill_id,
                       amount_cent, reason, requested_by_user_id, status, idempotency_key,
                       evidence_attachment_ids, snapshot_json, created_at
                  FROM bar_bill_operations
                 WHERE primary_bill_id = :bill_id OR secondary_bill_id = :bill_id OR generated_bill_id = :bill_id
                 ORDER BY created_at, id
                """, Map.of("bill_id", billId), rowMapper::mapBillOperation);
    }

    // ── V095 交班 / 班结对账 ──────────────────────────────────────────────────
    public BarShiftRecord saveShift(BarShiftRecord shift) {
        jdbcTemplate.update("""
                INSERT INTO bar_shifts (
                    id, project_id, merchant_id, bar_id, cashier_user_id, status,
                    opened_at, closed_at, opening_cash_cent, declared_cash_cent,
                    system_cash_cent, system_pos_cent, system_online_cent, system_refund_cent, system_tip_cent,
                    cash_diff_cent, cash_diff_threshold_cent, needs_review, handover_to_user_id, note,
                    opened_by_user_id, closed_by_user_id, created_at, updated_at
                )
                VALUES (
                    :id, :project_id, :merchant_id, :bar_id, :cashier_user_id, :status,
                    :opened_at, :closed_at, :opening_cash_cent, :declared_cash_cent,
                    :system_cash_cent, :system_pos_cent, :system_online_cent, :system_refund_cent, :system_tip_cent,
                    :cash_diff_cent, :cash_diff_threshold_cent, :needs_review, :handover_to_user_id, :note,
                    :opened_by_user_id, :closed_by_user_id, :created_at, :updated_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    status = EXCLUDED.status,
                    closed_at = EXCLUDED.closed_at,
                    declared_cash_cent = EXCLUDED.declared_cash_cent,
                    system_cash_cent = EXCLUDED.system_cash_cent,
                    system_pos_cent = EXCLUDED.system_pos_cent,
                    system_online_cent = EXCLUDED.system_online_cent,
                    system_refund_cent = EXCLUDED.system_refund_cent,
                    system_tip_cent = EXCLUDED.system_tip_cent,
                    cash_diff_cent = EXCLUDED.cash_diff_cent,
                    cash_diff_threshold_cent = EXCLUDED.cash_diff_threshold_cent,
                    needs_review = EXCLUDED.needs_review,
                    handover_to_user_id = EXCLUDED.handover_to_user_id,
                    note = EXCLUDED.note,
                    closed_by_user_id = EXCLUDED.closed_by_user_id,
                    updated_at = EXCLUDED.updated_at
                """, parameterSource.shiftParameters(shift));
        return shift;
    }

    public Optional<BarShiftRecord> findShiftById(String id) {
        return jdbcTemplate.query("SELECT * FROM bar_shifts WHERE id = :id",
                Map.of("id", id), rowMapper::mapShift).stream().findFirst();
    }

    public Optional<BarShiftRecord> findOpenShift(String barId, String cashierUserId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_shifts
                 WHERE bar_id = :bar_id AND cashier_user_id = :cashier_user_id AND status = 'open'
                 ORDER BY opened_at DESC
                 LIMIT 1
                """, Map.of("bar_id", barId, "cashier_user_id", cashierUserId), rowMapper::mapShift)
                .stream().findFirst();
    }

    public List<BarShiftRecord> listShifts(Optional<String> merchantScope) {
        if (merchantScope.isPresent()) {
            return jdbcTemplate.query("""
                    SELECT * FROM bar_shifts WHERE merchant_id = :merchant_id ORDER BY opened_at DESC, id
                    """, Map.of("merchant_id", merchantScope.get()), rowMapper::mapShift);
        }
        return jdbcTemplate.query("SELECT * FROM bar_shifts ORDER BY opened_at DESC, id", Map.of(), rowMapper::mapShift);
    }

    // P2 多综合体 project_id scoped 读通道：按真实 project_id 列做行级隔离（AC-ISO-1，§4）。既有查询不变。
    public List<BarShiftRecord> listShiftsByProject(String projectId) {
        return jdbcTemplate.query("""
                SELECT * FROM bar_shifts WHERE project_id = :project_id ORDER BY opened_at DESC, id
                """, Map.of("project_id", projectId), rowMapper::mapShift);
    }

    public BarShiftReconciliationSnapshot aggregateShiftReconciliation(
            String barId,
            String cashierUserId,
            Instant from,
            Instant to,
            Optional<String> merchantScope
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bar_id", barId)
                .addValue("cashier_user_id", cashierUserId)
                .addValue("from_ts", timestampWithTimeZone(from))
                .addValue("to_ts", timestampWithTimeZone(to))
                .addValue("merchant_id", merchantScope.orElse(null));
        String merchantFilter = merchantScope.isPresent() ? " AND b.merchant_id = :merchant_id" : "";
        // 各支付方式应收：本吧台 + 本收银员 + settled_at 落窗口的结算流水，按 settle_channel 归并。
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT
                  COALESCE(SUM(CASE WHEN s.settle_channel = 'cash' THEN s.settlement_amount_cent ELSE 0 END), 0) AS cash_cent,
                  COALESCE(SUM(CASE WHEN s.settle_channel = 'pos' THEN s.settlement_amount_cent ELSE 0 END), 0) AS pos_cent,
                  COALESCE(SUM(CASE WHEN s.settle_channel IN ('aggregate_pay', 'bank_transfer', 'other')
                                    THEN s.settlement_amount_cent ELSE 0 END), 0) AS online_cent
                  FROM bar_bill_settlements s
                  JOIN bar_bills b ON b.id = s.bill_id
                 WHERE b.bar_id = :bar_id
                   AND s.cashier_staff_id = :cashier_user_id
                   AND s.reversed = FALSE
                   AND s.settled_at >= :from_ts AND s.settled_at < :to_ts""" + merchantFilter,
                new HashMap<>(params.getValues()));
        long cash = ((Number) row.get("cash_cent")).longValue();
        long pos = ((Number) row.get("pos_cent")).longValue();
        long online = ((Number) row.get("online_cent")).longValue();
        // 退款：本吧台 + refunded_at 落窗口的 refund 调整额，净额扣减 refund_reversal（延期项④：
        // 退菜退酒审批驳回的退款冲销，加回应付并冲正退款台账，故经营日退款净额须相应抵减，不重复计入退款额）。
        Long refund = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(
                           CASE WHEN a.adjustment_type = 'refund' THEN a.amount_cent
                                WHEN a.adjustment_type = 'refund_reversal' THEN -a.amount_cent
                                ELSE 0 END), 0)
                  FROM bar_bill_adjustments a
                  JOIN bar_bills b ON b.id = a.bill_id
                 WHERE b.bar_id = :bar_id
                   AND a.adjustment_type IN ('refund', 'refund_reversal')
                   AND a.refunded_at >= :from_ts AND a.refunded_at < :to_ts""" + merchantFilter,
                new HashMap<>(params.getValues()), Long.class);
        // 小费：本吧台 + 本收银员窗口内结算账单的 tip（代收，不计低消/提成基数）。
        Long tip = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(b.tip_amount_cent), 0)
                  FROM bar_bills b
                 WHERE b.bar_id = :bar_id
                   AND b.tip_amount_cent > 0
                   AND EXISTS (
                       SELECT 1 FROM bar_bill_settlements s
                        WHERE s.bill_id = b.id
                          AND s.cashier_staff_id = :cashier_user_id
                          AND s.reversed = FALSE
                          AND s.settled_at >= :from_ts AND s.settled_at < :to_ts
                   )""" + merchantFilter,
                new HashMap<>(params.getValues()), Long.class);
        return new BarShiftReconciliationSnapshot(cash, pos, online,
                refund == null ? 0L : refund, tip == null ? 0L : tip);
    }

    public BarCommissionCalculationRecord saveCommissionCalculation(BarCommissionCalculationRecord calculation) {
        jdbcTemplate.update("""
                INSERT INTO bar_commission_calculations (
                    id, bill_id, sales_staff_id, sales_role_type, commission_base_cent,
                    commission_rate_bp, commission_amount_cent, status, created_at
                )
                VALUES (
                    :id, :bill_id, :sales_staff_id, :sales_role_type, :commission_base_cent,
                    :commission_rate_bp, :commission_amount_cent, :status, :created_at
                )
                ON CONFLICT (id) DO UPDATE SET
                    sales_staff_id = EXCLUDED.sales_staff_id,
                    sales_role_type = EXCLUDED.sales_role_type,
                    commission_base_cent = EXCLUDED.commission_base_cent,
                    commission_rate_bp = EXCLUDED.commission_rate_bp,
                    commission_amount_cent = EXCLUDED.commission_amount_cent,
                    status = EXCLUDED.status
                """, parameterSource.commissionParameters(calculation));
        return calculation;
    }
}
