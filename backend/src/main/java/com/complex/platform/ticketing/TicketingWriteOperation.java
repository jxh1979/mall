package com.complex.platform.ticketing;

import com.complex.platform.evidence.AuditedOperation;

enum TicketingWriteOperation implements AuditedOperation {
    CREATE_DYNAMIC_PRICING_RULE("ticket_dynamic_pricing.changed", "ticket_dynamic_pricing.change_failed", "ticket.dynamic_pricing.manage", "ticket_dynamic_pricing_rule"),
    PUBLISH_DYNAMIC_PRICING_RULE("ticket_dynamic_pricing.published", "ticket_dynamic_pricing.publish_failed", "ticket.dynamic_pricing.publish", "ticket_dynamic_pricing_rule"),
    SIMULATE_DYNAMIC_PRICING("ticket_dynamic_pricing.simulated", "ticket_dynamic_pricing.simulate_failed", "ticket.dynamic_pricing.simulate", "ticket_dynamic_pricing_simulation"),
    CREATE_TICKET_TYPE("ticket_product.changed", "ticket_product.change_failed", "ticket.product.manage", "ticket_type"),
    CREATE_TICKET_PRICE_RULE("ticket_price_variant.changed", "ticket_price_variant.change_failed", "ticket.price_variant.manage", "ticket_price_rule"),
    ARCHIVE_TICKET_TYPE("ticket_product.archived", "ticket_product.archive_failed", "ticket.product.manage", "ticket_type"),
    UNARCHIVE_TICKET_TYPE("ticket_product.unarchived", "ticket_product.unarchive_failed", "ticket.product.manage", "ticket_type"),
    REQUEST_DELETE_TICKET_TYPE("ticket_product.delete_requested", "ticket_product.delete_request_failed", "ticket.product.manage", "ticket_type"),
    APPROVE_DELETE_TICKET_TYPE("ticket_product.delete_approved", "ticket_product.delete_approve_failed", "ticket.type.approve", "ticket_type"),
    ARCHIVE_TICKET_PRICE_RULE("ticket_price_variant.archived", "ticket_price_variant.archive_failed", "ticket.price_variant.manage", "ticket_price_rule"),
    UNARCHIVE_TICKET_PRICE_RULE("ticket_price_variant.unarchived", "ticket_price_variant.unarchive_failed", "ticket.price_variant.manage", "ticket_price_rule"),
    REQUEST_DELETE_TICKET_PRICE_RULE("ticket_price_variant.delete_requested", "ticket_price_variant.delete_request_failed", "ticket.price_variant.manage", "ticket_price_rule"),
    APPROVE_DELETE_TICKET_PRICE_RULE("ticket_price_variant.delete_approved", "ticket_price_variant.delete_approve_failed", "ticket.price_rule.approve", "ticket_price_rule"),
    CREATE_ENTITLEMENT_TEMPLATE("ticket_entitlement_template.changed", "ticket_entitlement_template.changed_failed", "ticket.entitlement.manage", "entitlement_template"),
    ARCHIVE_ENTITLEMENT_TEMPLATE("ticket_entitlement_template.archived", "ticket_entitlement_template.archive_failed", "ticket.entitlement.manage", "entitlement_template"),
    UNARCHIVE_ENTITLEMENT_TEMPLATE("ticket_entitlement_template.unarchived", "ticket_entitlement_template.unarchive_failed", "ticket.entitlement.manage", "entitlement_template"),
    REQUEST_DELETE_ENTITLEMENT_TEMPLATE("ticket_entitlement_template.delete_requested", "ticket_entitlement_template.delete_request_failed", "ticket.entitlement.manage", "entitlement_template"),
    APPROVE_DELETE_ENTITLEMENT_TEMPLATE("ticket_entitlement_template.delete_approved", "ticket_entitlement_template.delete_approve_failed", "ticket.entitlement.approve", "entitlement_template"),
    BIND_TICKET_TYPE_ENTITLEMENT("ticket_product_entitlement.bound", "ticket_product_entitlement.bound_failed", "ticket.product.manage", "ticket_type_entitlement"),
    CREATE_TICKET_ORDER("ticket_order.created", "ticket_order.create_failed", "ticket.order.create", "ticket_order"),
    CREATE_TICKET_ORDER_PAY_INTENT("payment.intent_created", "payment.intent_failed", "finance.payment_intent.create", "ticket_payment_intent"),
    CONFIRM_TICKET_ORDER_PAYMENT("ticket_order.payment_confirmed", "ticket_order.payment_confirm_failed", "finance.payment.register", "ticket_order"),
    CANCEL_TICKET_ORDER("ticket_order.cancelled", "ticket_order.cancel_failed", "ticket.order.create", "ticket_order"),
    ISSUE_TICKET_ORDER("ticket.issued", "ticket.issue_failed", "ticket.ticket.issue", "issued_entitlement"),
    PUBLISH_TICKET_TYPE("ticket_price.published", "ticket_price.publish_failed", "ticket.product.manage", "ticket_type"),
    REQUEST_TICKET_REFUND("ticket_refund.requested", "ticket_refund.request_failed", "ticket.refund.request", "ticket_refund"),
    APPROVE_TICKET_REFUND("ticket_refund.approved", "ticket_refund.approve_failed", "ticket.refund.approve", "ticket_refund"),
    REJECT_TICKET_REFUND("ticket_refund.rejected", "ticket_refund.reject_failed", "ticket.refund.approve", "ticket_refund"),
    PREVIEW_VERIFICATION("verification.previewed", "verification.failed", "verification.execute", "ticket_verification"),
    CREATE_VERIFICATION("verification.succeeded", "verification.failed", "verification.execute", "ticket_verification"),
    REVERSE_VERIFICATION("ticket.verification_reversed", "ticket.verification_reverse_failed", "ticket.verify.reverse", "ticket_verification"),
    SYNC_TERMINAL_TASKS("terminal.sync_task.accepted", "terminal.sync_task.accepted_failed", "terminal.device.bind", "terminal_sync_task");

    private final String successEvent;
    private final String failureEvent;
    private final String permissionCode;
    private final String businessType;

    TicketingWriteOperation(String successEvent, String failureEvent, String permissionCode, String businessType) {
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
