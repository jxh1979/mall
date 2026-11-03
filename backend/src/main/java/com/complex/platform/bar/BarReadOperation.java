package com.complex.platform.bar;

// 文件版本：V0.03

import com.complex.platform.evidence.AuditedOperation;

enum BarReadOperation implements AuditedOperation {
    LIST_ORDERS("bar.order.viewed", "bar.order.view_failed", "bar.order.view", "bar_order"),
    LIST_PRODUCTION_TICKETS("bar.production_ticket.viewed", "bar.production_ticket.viewed_failed", "bar.production_ticket.view", "bar_production_ticket"),
    LIST_PRODUCTION_TICKET_EVENTS("bar.production_ticket_event.list", "bar.production_ticket_event.list_failed", "bar.production_ticket.view", "bar_production_ticket_event"),
    LIST_PRODUCTS("bar.product.viewed", "bar.product.viewed_failed", "bar.order.create", "bar_product"),
    LIST_RESERVATIONS("bar.reservation.viewed", "bar.reservation.view_failed", "bar.reservation.view", "bar_reservation"),
    // 第十八批 C 段第 15 条：代订权限停用中的经手员工（店长复核用）。
    LIST_STAFF_BOOKING_SUSPENSIONS("bar.staff_booking_suspension.viewed", "bar.staff_booking_suspension.view_failed", "bar.staff_booking.restore", "bar_staff_booking_suspension"),
    LIST_TABLES("bar.table.viewed", "bar.table.view_failed", "bar.table.view", "bar_table"),
    // BYOD 一期:空位视图凭 bar.reservation.create(能订位自然能看空位),不放大到桌位管理。
    LIST_TABLE_AVAILABILITY("bar.table_availability.viewed", "bar.table_availability.view_failed", "bar.reservation.create", "bar_table"),
    LIST_TABLE_SESSIONS("bar.table_session.list", "bar.table_session.list_failed", "bar.table_session.view", "bar_table_session"),
    LIST_BILL_OPERATIONS("bar.bill_operation.list", "bar.bill_operation.list_failed", "bar.bill.split_merge", "bar_bill_operation"),
    // 第十一批 TK-04-R5：账单端反查本单商户消费奖励（凭查看订单账单的 bar.order.view，限本商户）。
    LIST_BILL_REWARDS("bar.bill_rewards.viewed", "bar.bill_rewards.view_failed", "bar.order.view", "merchant_consumption_reward"),
    LIST_TABLES_WITHOUT_SPACE("bar.table_space_backlog.viewed", "bar.table_space_backlog.view_failed", "bar.table.manage", "bar_table"),
    LIST_SHIFTS("bar.shift.viewed", "bar.shift.view_failed", "bar.shift.manage", "bar_shift"),
    LIST_GIFT_REDEMPTIONS("bar.entitlement_redemption.viewed", "bar.entitlement_redemption.view_failed", "bar.entitlement.read", "bar_gift_redemption"),
    // V345 退酒审批待审队列：凭 bar.order_item.void(退菜退酒域权限)可看，申请人看自己申请的状态、
    // 主管在审批前看清单;真正的审批动作另需 bar.order_item.void_decide(V125 已分离),读写权限不混。
    LIST_ORDER_ITEM_VOIDS("bar.order_item_void.viewed", "bar.order_item_void.view_failed", "bar.order_item.void", "bar_order_item_void");

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
