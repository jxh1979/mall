package com.complex.platform.ticketing;

// 文件版本：V0.03

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
    // 配置中心批5：改虚拟权益签约价（资金口径，理由必填；只对之后发放的权益生效，历史按快照不动）。
    // 批10/E：UPDATE_ENTITLEMENT_TEMPLATE_AGREEMENT_PRICE 已下线(改价收编进内部结算 SKU 目录)。
    BIND_TICKET_TYPE_ENTITLEMENT("ticket_product_entitlement.bound", "ticket_product_entitlement.bound_failed", "ticket.product.manage", "ticket_type_entitlement"),
    CREATE_TICKET_ORDER("ticket_order.created", "ticket_order.create_failed", "ticket.order.create", "ticket_order"),
    CREATE_TICKET_ORDER_PAY_INTENT("payment.intent_created", "payment.intent_failed", "finance.payment_intent.create", "ticket_payment_intent"),
    CONFIRM_TICKET_ORDER_PAYMENT("ticket_order.payment_confirmed", "ticket_order.payment_confirm_failed", "finance.payment.register", "ticket_order"),
    CANCEL_TICKET_ORDER("ticket_order.cancelled", "ticket_order.cancel_failed", "ticket.order.create", "ticket_order"),
    ISSUE_TICKET_ORDER("ticket.issued", "ticket.issue_failed", "ticket.ticket.issue", "issued_entitlement"),
    // V369 改票种售价/折扣(综合/联合通票售价自动算 + 凑整误差闸):保存当场比对允许误差,超出即拒。
    UPDATE_TICKET_TYPE_PRICING("ticket_product.pricing_changed", "ticket_product.pricing_change_failed", "ticket.product.manage", "ticket_type"),
    PUBLISH_TICKET_TYPE("ticket_price.published", "ticket_price.publish_failed", "ticket.product.manage", "ticket_type"),
    REQUEST_TICKET_REFUND("ticket_refund.requested", "ticket_refund.request_failed", "ticket.refund.request", "ticket_refund"),
    APPROVE_TICKET_REFUND("ticket_refund.approved", "ticket_refund.approve_failed", "ticket.refund.approve", "ticket_refund"),
    REJECT_TICKET_REFUND("ticket_refund.rejected", "ticket_refund.reject_failed", "ticket.refund.approve", "ticket_refund"),
    // 退款实际拨付确认（business-enum 退款单状态：只有“成功”才计入已退金额）：approved→success。
    CONFIRM_TICKET_REFUND_PAYOUT("ticket_refund.payout_confirmed", "ticket_refund.payout_confirm_failed", "ticket.refund.approve", "ticket_refund"),
    // TK-05-R2/R3 通票安全例外（BSM:804）：登记与夜班值班经理及以上批准。
    REQUEST_TICKET_SAFETY_EVENT("ticket_safety_event.requested", "ticket_safety_event.request_failed", "ticket.refund.request", "ticket_safety_event"),
    APPROVE_TICKET_SAFETY_EVENT("ticket_safety_event.approved", "ticket_safety_event.approve_failed", "ticket.refund.approve", "ticket_safety_event"),
    PREVIEW_VERIFICATION("verification.previewed", "verification.failed", "verification.execute", "ticket_verification"),
    CREATE_VERIFICATION("verification.succeeded", "verification.failed", "verification.execute", "ticket_verification"),
    REVERSE_VERIFICATION("ticket.verification_reversed", "ticket.verification_reverse_failed", "ticket.verify.reverse", "ticket_verification"),
    // TK-14（ENUM「核销记录状态」「异常状态的统一出口」）：成功核销提起异常复核 / 主管二选一处置。
    RAISE_VERIFICATION_ANOMALY("ticket.verification_anomaly_raised", "ticket.verification_anomaly_raise_failed", "verification.execute", "ticket_verification"),
    RESOLVE_VERIFICATION_ANOMALY("ticket.verification_anomaly_resolved", "ticket.verification_anomaly_resolve_failed", "ticket.verify.anomaly.resolve", "ticket_verification"),
    SYNC_TERMINAL_TASKS("terminal.sync_task.accepted", "terminal.sync_task.accepted_failed", "terminal.device.bind", "terminal_sync_task"),
    // AUD-006 转赠发起/领取走 executeWrite 幂等+审计(事件名与 openapi x-audit-event 对齐;客人端权限=not_applicable)。
    INITIATE_TICKET_TRANSFER("ticket_transfer.created", "ticket_transfer.create_failed", "not_applicable", "ticket_transfer"),
    CLAIM_TICKET_TRANSFER("ticket_transfer.claimed", "ticket_transfer.claim_failed", "not_applicable", "ticket_transfer"),
    // #172(2026-07-19 拍板)：后台改全场统一动态码刷新间隔（走 executeWrite 幂等+审计）。
    UPDATE_QR_REFRESH_CONFIG("ticket.qr_refresh_config.updated", "ticket.qr_refresh_config.update_failed", "ticket.product.manage", "ticket_qr_refresh_config");

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
