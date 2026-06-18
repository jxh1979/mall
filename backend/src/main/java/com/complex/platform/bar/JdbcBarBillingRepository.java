package com.complex.platform.bar;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                    settlement_status, sales_staff_id, sales_role_type, created_at, updated_at
                )
                VALUES (
                    :id, :bill_no, :order_id, :bar_id, :merchant_id, :table_id, :reservation_id,
                    :subtotal_amount_cent, :discount_amount_cent, :deposit_deduct_amount_cent,
                    :payable_amount_cent, :paid_amount_cent, :currency, :payment_status,
                    :settlement_status, :sales_staff_id, :sales_role_type, :created_at, :updated_at
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
                    updated_at = EXCLUDED.updated_at
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
                       settlement_status, sales_staff_id, sales_role_type, created_at, updated_at
                  FROM bar_bills
                 WHERE id = :id
                """, Map.of("id", id), rowMapper::mapBill).stream().findFirst();
    }

    public BarBillAdjustmentRecord saveBillAdjustment(BarBillAdjustmentRecord adjustment) {
        jdbcTemplate.update("""
                INSERT INTO bar_bill_adjustments (
                    id, bill_id, adjustment_type, amount_cent,
                    before_payable_amount_cent, after_payable_amount_cent,
                    reason, requested_by_user_id, approval_required, status,
                    idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json, created_at
                )
                VALUES (
                    :id, :bill_id, :adjustment_type, :amount_cent,
                    :before_payable_amount_cent, :after_payable_amount_cent,
                    :reason, :requested_by_user_id, :approval_required, :status,
                    :idempotency_key, CAST(:evidence_attachment_ids AS jsonb),
                    :ar_ap_item_id, CAST(:snapshot_json AS jsonb), :created_at
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
                    snapshot_json = EXCLUDED.snapshot_json
                """, parameterSource.billAdjustmentParameters(adjustment));
        return adjustment;
    }

    public List<BarBillAdjustmentRecord> listBillAdjustments(String billId) {
        return jdbcTemplate.query("""
                SELECT id, bill_id, adjustment_type, amount_cent,
                       before_payable_amount_cent, after_payable_amount_cent,
                       reason, requested_by_user_id, approval_required, status,
                       idempotency_key, evidence_attachment_ids, ar_ap_item_id, snapshot_json, created_at
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
