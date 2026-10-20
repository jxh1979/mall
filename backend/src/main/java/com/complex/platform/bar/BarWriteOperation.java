package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.evidence.AuditedOperation;

enum BarWriteOperation implements AuditedOperation {
    CREATE_ORDER("bar.order_created", "bar.order_create_failed", "bar.order.create", "bar_order"),
    SCAN_ORDER("bar.scan_order_created", "bar.scan_order_create_failed", "bar.order.create", "bar_order"),
    CREATE_ORDER_BILL("bar.bill.created", "bar.bill.created_failed", "bar.order.create", "bar_bill"),
    DELIVER_ORDER("bar.order_delivered", "bar.order_deliver_failed", "bar.order.create", "bar_order"),
    ADD_ORDER_ITEM("bar.order_item.created", "bar.order_item.created_failed", "bar.order.create", "bar_order_item"),
    TRANSITION_PRODUCTION_TICKET("bar.production_ticket.transitioned", "bar.production_ticket.transition_failed", "bar.production_ticket.manage", "bar_production_ticket"),
    CREATE_PRODUCT("bar.product.created", "bar.product.create_failed", "bar.order.create", "bar_product"),
    CREATE_RESERVATION("bar.reservation.created", "bar.reservation.create_failed", "bar.reservation.create", "bar_reservation"),
    PAY_RESERVATION_DEPOSIT("bar.reservation_deposit_paid", "bar.reservation_deposit_pay_failed", "bar.reservation.manage", "bar_reservation"),
    // 第十批 TK-24-R1：到店只留痕、定金不再抵扣（原 bar.reservation_deposit_deducted 事件停用）。
    CHECK_IN_RESERVATION_DEPOSIT("bar.reservation_checked_in", "bar.reservation_checkin_failed", "bar.reservation.manage", "bar_reservation"),
    CANCEL_RESERVATION("bar.reservation.cancelled", "bar.reservation.cancel_failed", "bar.reservation.manage", "bar_reservation"),
    CREATE_DEPOSIT_RULE("bar.deposit_rule.created", "bar.deposit_rule.create_failed", "bar.deposit_rule.manage", "bar_deposit_rule"),
    VERIFY_RESERVATION_ENTRY_ALLOWANCE("bar.table_guest.entry_allowance_verified", "bar.table_guest.entry_allowance_verify_failed", "bar.table.guest_checkin", "bar_reservation"),
    CHECK_IN_RESERVATION_GUEST("bar.table_guest.checked_in", "bar.table_guest.rejected", "bar.table.guest_checkin", "bar_table_guest_checkin"),
    CREATE_TABLE("bar.table.changed", "bar.table.change_failed", "bar.table.manage", "bar_table"),
    ASSIGN_TABLE_SPACE("bar.table_space.assigned", "bar.table_space.assign_failed", "bar.table.manage", "bar_table"),
    OPEN_TABLE("bar.table.opened", "bar.table.open_failed", "bar.table.open_close", "bar_table"),
    // 第十批 TK-27-R2：清台登记恢复空闲（BSM L1058）。
    CLEAR_TABLE("bar.table.cleared", "bar.table.clear_failed", "bar.table.open_close", "bar_table"),
    CREATE_TABLE_SESSION("bar.table_session.created", "bar.table_session.create_failed", "bar.table_session.create", "bar_table_session"),
    TRANSFER_TABLE_SESSION("bar.table.transferred", "bar.table.transfer_failed", "bar.table_session.transfer", "bar_table_session_adjustment"),
    MERGE_TABLE_SESSIONS("bar.table.merged", "bar.table.merge_failed", "bar.table_session.merge", "bar_table_session_adjustment"),
    RENEW_TABLE_SESSION("bar.table.renewed", "bar.table.renew_failed", "bar.table_session.renew", "bar_table_session_adjustment"),
    // 第十批 TK-27-R3：拆台（BSM L1079）。
    SPLIT_TABLE_SESSIONS("bar.table.split", "bar.table.split_failed", "bar.table_session.split", "bar_table_session_adjustment"),
    CREATE_BILL_PAY_INTENT("payment.intent_created", "payment.intent_failed", "finance.payment_intent.create", "finance_payment_intent"),
    SETTLE_BILL("bar.bill.settled", "bar.bill.settle_failed", "finance.payment.register", "bar_bill"),
    APPLY_BILL_ADJUSTMENT("bar.bill.adjusted", "bar.bill.adjust_failed", "bar.bill.adjust", "bar_bill_adjustment"),
    SPLIT_BILL("bar.bill.split", "bar.bill.split_failed", "bar.bill.split_merge", "bar_bill_operation"),
    MERGE_BILLS("bar.bill.merged", "bar.bill.merge_failed", "bar.bill.split_merge", "bar_bill_operation"),
    CALCULATE_COMMISSION("bar.table_commission.calculated", "bar.table_commission.calculate_failed", "bar.table_commission.calculate", "bar_commission_calculation"),
    VOID_ORDER_ITEM("bar.order_item_voided", "bar.order_item_void_failed", "bar.order_item.void", "bar_order_item"),
    DECIDE_ORDER_ITEM_VOID("bar.order_item_void_decided", "bar.order_item_void_decide_failed", "bar.order_item.void", "bar_order_item_void"),
    REOPEN_BILL("bar.bill_reopened", "bar.bill_reopen_failed", "bar.bill.reopen", "bar_bill"),
    MARK_PRODUCT_SOLD_OUT("bar.product_sold_out", "bar.product_sold_out_failed", "bar.product.sold_out", "bar_product"),
    ADD_BILL_TIP("bar.bill_tip_added", "bar.bill_tip_failed", "bar.bill.tip", "bar_bill"),
    OPEN_SHIFT("bar.shift_opened", "bar.shift_open_failed", "bar.shift.manage", "bar_shift"),
    CLOSE_SHIFT("bar.shift_closed", "bar.shift_close_failed", "bar.shift.close", "bar_shift"),
    REDEEM_DRINK_ENTITLEMENT("bar.entitlement_redeemed", "bar.entitlement_redeem_failed", "bar.entitlement.redeem", "bar_gift_redemption"),
    ISSUE_INVITATION("bar.reservation_invitation_issued", "bar.reservation_invitation_issue_failed", "bar.reservation.invitation.manage", "bar_reservation"),
    REVOKE_INVITATION("bar.reservation_invitation_revoked", "bar.reservation_invitation_revoke_failed", "bar.reservation.invitation.manage", "bar_reservation"),
    ENTER_INVITATION("bar.reservation_invitation_entered", "bar.reservation_invitation_entry_failed", "bar.table.guest_checkin", "bar_reservation"),
    REQUEST_DELETE_BAR_CATEGORY("bar.category.delete_requested", "bar.category.delete_request_failed", "bar.category.manage", "bar_product_category"),
    APPROVE_DELETE_BAR_CATEGORY("bar.category.delete_approved", "bar.category.delete_approve_failed", "bar.category.approve", "bar_product_category"),
    REQUEST_DELETE_BAR_PRODUCT("bar.product.delete_requested", "bar.product.delete_request_failed", "bar.product.manage", "bar_product"),
    APPROVE_DELETE_BAR_PRODUCT("bar.product.delete_approved", "bar.product.delete_approve_failed", "bar.product.approve", "bar_product"),
    REQUEST_DELETE_BAR_COMBO("bar.combo.delete_requested", "bar.combo.delete_request_failed", "bar.combo.manage", "bar_combo"),
    APPROVE_DELETE_BAR_COMBO("bar.combo.delete_approved", "bar.combo.delete_approve_failed", "bar.combo.approve", "bar_combo"),
    REQUEST_DELETE_BAR_RESERVATION("bar.reservation.delete_requested", "bar.reservation.delete_request_failed", "bar.reservation.manage", "bar_reservation"),
    APPROVE_DELETE_BAR_RESERVATION("bar.reservation.delete_approved", "bar.reservation.delete_approve_failed", "bar.reservation.approve", "bar_reservation");

    private final String successEvent;
    private final String failureEvent;
    private final String permissionCode;
    private final String businessType;

    BarWriteOperation(String successEvent, String failureEvent, String permissionCode, String businessType) {
        this.successEvent = successEvent;
        this.failureEvent = failureEvent;
        this.permissionCode = permissionCode;
        this.businessType = businessType;
    }

    public String successEvent() {
        return successEvent;
    }

    public String failureEvent() {
        return failureEvent;
    }

    public String permissionCode() {
        return permissionCode;
    }

    public String businessType() {
        return businessType;
    }
}
