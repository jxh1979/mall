package com.complex.platform.customer.dto;

// 文件版本：V0.01

import com.complex.platform.ticketing.TicketOrderRecord;
import com.complex.platform.ticketing.TicketPaymentIntentRecord;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 客人「我的订单」视图：订单原字段平铺，另带 ROOT-03-R20 组合支付的已付、待付与已收各笔（BSM 第 859 行「查看支付结果」），
 * 以及 ROOT-03-R27 全部命中资格价（BSM 第 756 行「订单保存……全部命中资格价、最终选中价和规则版本」）。
 */
public record CustomerTicketOrderResponse(
        @JsonUnwrapped TicketOrderRecord order,
        long paidAmountCent,
        long remainingCent,
        List<Payment> payments,
        List<Map<String, Object>> priceHits
) {
    /** 已收的一笔：渠道（stored_value＝储值、wechat、alipay 等）、金额、状态与到账时刻。 */
    public record Payment(String channel, long amountCent, String status, Instant paidAt) {
    }

    /** 待付＝未付订单的订单金额减已收各笔；已付或已取消的为 0。 */
    public static CustomerTicketOrderResponse of(TicketOrderRecord order, List<TicketPaymentIntentRecord> received,
            List<Map<String, Object>> priceHits) {
        long paid = received.stream().mapToLong(TicketPaymentIntentRecord::amountCent).sum();
        boolean open = "unpaid".equals(order.payStatus()) && !"cancelled".equals(order.status());
        return new CustomerTicketOrderResponse(order, paid, open ? Math.max(0L, order.amountCent() - paid) : 0L,
                received.stream().map(p -> new Payment(p.channel(), p.amountCent(), p.status(), p.updatedAt())).toList(),
                priceHits);
    }
}
