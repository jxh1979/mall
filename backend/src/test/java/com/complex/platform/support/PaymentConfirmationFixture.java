package com.complex.platform.support;

// 文件版本：V0.02

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批 AM 段第 241 条 241-R1（第十八批裁决 55「所有应付都先走付款确认」）出款闸对既有用例的前提夹具：直插一行已通过的付款确认（复核单号以
 * review_fixture_ 开头，不建复核单、不走审批链），让既有的登记付款、发起内部付款等用例回到原意图。只给付款确认以外的用例用；发起与逐级审批走真实
 * 接口的用例见 PaymentConfirmationJdbcTests。同一对象已有待审或已通过的确认时不重插。
 */
public final class PaymentConfirmationFixture {

    /** 台账来源即支付类型（裁决 65）；V629 闭集里的台账来源。 */
    private static final List<String> AR_AP_SOURCES = List.of(
            "dividend_payout", "dividend_reserve_released", "staff_payroll", "deposit_refund", "deposit_adjustment",
            "purchase_inbound", "purchase_consignment", "purchase_prepayment", "ticket_distribution", "platform_incentive_subsidy",
            "single_bar_service_fee", "compensation_fulfillment", "pass_domain_share", "pass_domain_direct_settlement",
            "pass_unverified_attribution", "dispatch_commission_transfer", "bar_payroll_transfer", "inventory_transfer",
            "pass_drink_settlement", "media_performance_settlement", "refund", "bar_deposit_refund", "stored_value_consume",
            "partner_voucher", "reconciliation_difference", "inventory_difference", "turnover_rent_reversal", "manual", "bar_loan",
            "compensation_fulfillment_recovery", "ticket_distribution_recovery", "saved_bottle_remedy_recovery");

    private PaymentConfirmationFixture() {
    }

    public static void approved(JdbcTemplate jdbc, String objectType, String objectId, long amountCent, String paymentType) {
        String token = UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO payment_confirmations (id, confirmation_id, payment_type, object_type, object_id, amount_cent, reason, status,"
                        + " review_id, requested_by, decided_at)"
                        + " SELECT ?, ?, ?, ?, ?, ?, '测试夹具：已通过的付款确认', 'approved', ?, 'usr_payment_confirmation_fixture', clock_timestamp()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM payment_confirmations WHERE object_type = ? AND object_id = ?"
                        + " AND status IN ('pending_review', 'approved'))",
                "pcl_fx_" + token, "pc_fx_" + token, paymentType, objectType, objectId, Math.max(amountCent, 1L), "review_fixture_" + token,
                objectType, objectId);
    }

    /** 第十八批 AM 段第 241 条 241-R5：财务退款待办，支付类型取 finance_refund_＋其来源，金额取退款金额。 */
    public static void approvedFinanceRefund(JdbcTemplate jdbc, String refundId) {
        var row = jdbc.queryForMap("SELECT source_type, refund_amount_cent FROM finance_refunds WHERE id = ?", refundId);
        approved(jdbc, "finance_refund", refundId, ((Number) row.get("refund_amount_cent")).longValue(), "finance_refund_" + row.get("source_type"));
    }

    /** 台账款项：支付类型取其来源（来源不在清单里的手工建账夹具按 manual），金额取总额。 */
    public static void approvedArAp(JdbcTemplate jdbc, String arApItemId) {
        var row = jdbc.queryForMap("SELECT source_type, amount_cent FROM ar_ap_items WHERE id = ?", arApItemId);
        String source = (String) row.get("source_type");
        approved(jdbc, "ar_ap_item", arApItemId, ((Number) row.get("amount_cent")).longValue(),
                source != null && AR_AP_SOURCES.contains(source) ? source : "manual");
    }
}
