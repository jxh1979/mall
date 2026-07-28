package com.complex.platform.customer.dto;

import java.time.LocalDate;
import java.util.List;

public record CustomerTicketOrderRequest(
        String idempotencyKey,
        String ticketTypeId,
        String priceRuleId,
        Integer quantity,
        LocalDate visitDate,
        String contactPhone,
        String buyerNote,
        // AUD-076 单店票挂酒吧：客人选中的酒吧商户 id（单店票必填、通票必空；服务端据票种校验并快照）。
        String barId,
        // V359 客人端下单拍脸照：需拍脸照的票种（require_face_photo，游乐票默认开）在 H5 下单时逐张拍，
        //   base64 随下单请求上来，加密暂存到订单，后台出票时转绑到已发权益。其余票种为空、零影响。
        List<String> facePhotos
) {
    // ponytail: V359 前带 barId 但不带 facePhotos 的调用点 → 空表(=不拍脸照)。
    public CustomerTicketOrderRequest(
            String idempotencyKey, String ticketTypeId, String priceRuleId, Integer quantity,
            LocalDate visitDate, String contactPhone, String buyerNote, String barId) {
        this(idempotencyKey, ticketTypeId, priceRuleId, quantity, visitDate, contactPhone, buyerNote, barId,
                List.of());
    }

    // AUD-076 7 参兼容构造（无 barId）→ barId=null（既有测试调用点零改动）。
    public CustomerTicketOrderRequest(
            String idempotencyKey, String ticketTypeId, String priceRuleId, Integer quantity,
            LocalDate visitDate, String contactPhone, String buyerNote) {
        this(idempotencyKey, ticketTypeId, priceRuleId, quantity, visitDate, contactPhone, buyerNote, null);
    }
}
