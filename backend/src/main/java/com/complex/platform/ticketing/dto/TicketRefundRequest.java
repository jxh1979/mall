package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

import java.util.List;

public record TicketRefundRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String targetId,
        Long refundAmountCent,
        String reason,
        String refundChannel,
        // 方案2 整票退款（AUD-006）已退役：退款单位改为父票实例（见 ticketInstanceIds）。
        // 本字段仅为契约过渡保留；传入非空即 400 REFUND_UNIT_IS_TICKET（退款单位为整张票，请按票选择）。
        List<String> entitlementIds,
        // 方案2 整票退款（AUD-006）：要退该订单下哪几张父票实例（ticket_instances ID）；不传/空=退该订单全部“完全未核销”的票。
        // 金额由服务端按各实例出票时定格的实付分摊额算定；父票任一权益已核销则整张不可退。
        List<String> ticketInstanceIds,
        // TK-05（BSM:804「只有票种规则明确允许、活动或场次取消、平台或履约方无法提供服务、重复扣款、法律法规要求这五种情形才允许退款」
        // 「五种情形之外只能由持授权 U 盘密钥的最高权限逐笔放行」）：ticket_type_rule / event_cancelled / service_unavailable /
        // duplicate_charge / legal_requirement / master_override 之一，必填。通票安全例外走安全事件审批入口，不在此提交。
        String reasonCode,
        // BSM:806「记录退款原因、责任主体」：主体主数据中在用的法律主体；每笔退款必填。
        String responsibleEntityId
) {
}
