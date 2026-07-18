package com.complex.platform.customer.dto;

/**
 * AUD-077 发起门票转赠。channel=phone 需 receiverPhone;channel=link(微信/二维码)服务端生成一次性领取码。
 * slotIndex 可空:单 slot 票(单人通票/多次票,num_people=1)服务端默认 slot 1;多人票必须指定要送哪一个人。
 */
public record CustomerTicketTransferRequest(
        String idempotencyKey,
        String parentTicketId,
        Integer slotIndex,
        String channel,
        String receiverPhone
) {
}
