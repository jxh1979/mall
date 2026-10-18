package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;
import java.time.LocalDate;

public record TicketOrderRecord(
        String id,
        String orderNo,
        String customerId,
        String ticketTypeId,
        String priceRuleId,
        int quantity,
        LocalDate visitDate,
        String salesChannel,
        String buyerPhone,
        long amountCent,
        String dynamicPricingRuleId,
        long baseAmountCent,
        long dynamicAdjustmentCent,
        String currency,
        String payStatus,
        String issueStatus,
        String status,
        Instant createdAt,
        Instant updatedAt,
        // 行级隔离（V135）：下单受理/销售商户；平台下单为 null（平台官方票务口径）。列表读取按此 scope 过滤。
        String merchantId,
        // V231 强制口径：支付成功时刻（pay_status→paid 时写入）；未支付为 null。与 createdAt（下单时间）严格区分。
        Instant paidAt,
        // AUD-076 单店票挂酒吧：下单时从票种快照的酒吧商户 id；null=通票/平台票。与 merchantId（受理商户）语义正交。
        String barId,
        // 售票时锁定的综合体；后续付款、退款和核销不得改取当前操作者或商户现归属。
        String projectId
) {
    public TicketOrderRecord(String id, String orderNo, String customerId, String ticketTypeId, String priceRuleId,
            int quantity, LocalDate visitDate, String salesChannel, String buyerPhone, long amountCent,
            String dynamicPricingRuleId, long baseAmountCent, long dynamicAdjustmentCent, String currency,
            String payStatus, String issueStatus, String status, Instant createdAt, Instant updatedAt,
            String merchantId, Instant paidAt, String barId) {
        this(id, orderNo, customerId, ticketTypeId, priceRuleId, quantity, visitDate, salesChannel, buyerPhone,
                amountCent, dynamicPricingRuleId, baseAmountCent, dynamicAdjustmentCent, currency,
                payStatus, issueStatus, status, createdAt, updatedAt, merchantId, paidAt, barId, null);
    }

    /**
     * XM-009（PGM-1.15.2.c2「员工日常界面只显示脱敏号码，不见明文；界面脱敏不提供关闭开关」）：员工端读端只出掩码购票人手机号。
     * 脱敏放在 controller（service 外）：服务层内部仍需明文（回写密文 / 转赠适配 / 储值扣款），与 V339 多主体联系人同款范式。
     */
    public TicketOrderRecord withMaskedPhone() {
        return new TicketOrderRecord(id, orderNo, customerId, ticketTypeId, priceRuleId, quantity, visitDate, salesChannel,
                com.complex.platform.common.PhoneMask.mask(buyerPhone), amountCent, dynamicPricingRuleId, baseAmountCent,
                dynamicAdjustmentCent, currency, payStatus, issueStatus, status, createdAt, updatedAt, merchantId, paidAt, barId, projectId);
    }
}
