package com.complex.platform.ticketing;

import com.complex.platform.evidence.AuditedOperation;

enum TicketingReadOperation implements AuditedOperation {
    LIST_TICKET_TYPES("ticket_product.viewed", "ticket_product.view_failed", "ticket.product.view", "ticket_type"),
    LIST_TICKET_PRICE_RULES("ticket_price_variant.viewed", "ticket_price_variant.view_failed", "ticket.price_variant.view", "ticket_price_rule"),
    LIST_DYNAMIC_PRICING_RULES("ticket_dynamic_pricing.viewed", "ticket_dynamic_pricing.view_failed", "ticket.dynamic_pricing.view", "ticket_dynamic_pricing_rule"),
    LIST_ENTITLEMENT_TEMPLATES("ticket_entitlement_template.viewed", "ticket_entitlement_template.viewed_failed", "ticket.entitlement.view", "entitlement_template"),
    LIST_TICKET_ORDERS("ticket_order.viewed", "ticket_order.view_failed", "ticket.order.read", "ticket_order"),
    LIST_ISSUED_ENTITLEMENTS("ticket_issued_entitlement.viewed", "ticket_issued_entitlement.viewed_failed", "ticket.verify.view", "issued_entitlement"),
    LIST_TICKET_INSTANCES("ticket_instance.viewed", "ticket_instance.viewed_failed", "ticket.verify.view", "ticket_instance"),
    LIST_TICKET_REFUND_LINES("ticket_refund_line.viewed", "ticket_refund_line.view_failed", "ticket.refund.view", "ticket_refund_line"),
    LIST_VERIFICATIONS("verification.viewed", "verification.view_failed", "verification.view", "ticket_verification"),
    LIST_TICKET_TRANSFERS("ticket_transfer.viewed", "ticket_transfer.view_failed", "ticket.transfer.read", "ticket_transfer");

    private final String successEvent;
    private final String failureEvent;
    private final String permissionCode;
    private final String businessType;

    TicketingReadOperation(String successEvent, String failureEvent, String permissionCode, String businessType) {
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
