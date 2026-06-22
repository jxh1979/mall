package com.complex.platform.bar;

import com.complex.platform.evidence.AuditedOperation;

enum BarReadOperation implements AuditedOperation {
    LIST_ORDERS("bar.order.viewed", "bar.order.view_failed", "bar.order.view", "bar_order"),
    LIST_PRODUCTION_TICKETS("bar.production_ticket.viewed", "bar.production_ticket.viewed_failed", "bar.production_ticket.view", "bar_production_ticket"),
    LIST_PRODUCTION_TICKET_EVENTS("bar.production_ticket_event.viewed", "bar.production_ticket_event.viewed_failed", "bar.production_ticket.view", "bar_production_ticket_event"),
    LIST_PRODUCTS("bar.product.viewed", "bar.product.viewed_failed", "bar.order.create", "bar_product"),
    LIST_RESERVATIONS("bar.reservation.viewed", "bar.reservation.view_failed", "bar.reservation.view", "bar_reservation"),
    LIST_TABLES("bar.table.viewed", "bar.table.view_failed", "bar.table.view", "bar_table"),
    LIST_TABLE_SESSIONS("bar.table_session.viewed", "bar.table_session.view_failed", "bar.table_session.view", "bar_table_session"),
    LIST_BILL_OPERATIONS("bar.bill_operations.viewed", "bar.bill_operations.view_failed", "bar.bill.split_merge", "bar_bill_operation"),
    LIST_TABLES_WITHOUT_SPACE("bar.table_space_backlog.viewed", "bar.table_space_backlog.view_failed", "bar.table.manage", "bar_table"),
    LIST_SHIFTS("bar.shift.viewed", "bar.shift.view_failed", "bar.shift.manage", "bar_shift"),
    LIST_GIFT_REDEMPTIONS("bar.entitlement_redemption.viewed", "bar.entitlement_redemption.view_failed", "bar.entitlement.read", "bar_gift_redemption");

    private final String successEvent;
    private final String failureEvent;
    private final String permissionCode;
    private final String businessType;

    BarReadOperation(String successEvent, String failureEvent, String permissionCode, String businessType) {
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
