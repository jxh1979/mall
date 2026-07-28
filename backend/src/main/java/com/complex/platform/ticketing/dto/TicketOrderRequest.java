package com.complex.platform.ticketing.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record TicketOrderRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketTypeId,
        String priceRuleId,
        Integer quantity,
        LocalDate visitDate,
        String salesChannel,
        String buyerPhone,
        String customerId,
        Map<String, Object> dynamicPricingContext,
        // AUD-076 单店票挂酒吧：客人下单选中的酒吧商户 id；服务端据票种 bar_id 校验并快照，客户端值仅作核对。
        String barId,
        // V359 客人端下单拍脸照：本单每张票一张脸照(base64)，加密暂存到订单，后台出票时转绑到已发权益。
        //   仅 require_face_photo 票种要求；其余票种此表为空、零影响。张数上限=quantity。
        List<String> facePhotos,
        // V368 人脸照同意留痕 id(customer_consents.id)：facePhotos 非空时必填，服务端生成、不接受客户端值。
        //   照片落库时挂上它——库上 chk_tfp_consent_present 保证「订单暂存位有照必有同意」，谁都绕不过去。
        String faceConsentId
) {
    // ponytail: V359 前带 barId 但不带 facePhotos 的调用点 → 空表(=不拍脸照)。
    public TicketOrderRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String ticketTypeId,
            String priceRuleId,
            Integer quantity,
            LocalDate visitDate,
            String salesChannel,
            String buyerPhone,
            String customerId,
            Map<String, Object> dynamicPricingContext,
            String barId
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketTypeId, priceRuleId, quantity,
                visitDate, salesChannel, buyerPhone, customerId, dynamicPricingContext, barId, List.of(), null);
    }

    // AUD-076 11 参兼容构造（无 barId）→ barId=null（既有员工/终端/测试调用点零改动）。
    public TicketOrderRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String ticketTypeId,
            String priceRuleId,
            Integer quantity,
            LocalDate visitDate,
            String salesChannel,
            String buyerPhone,
            String customerId,
            Map<String, Object> dynamicPricingContext
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketTypeId, priceRuleId, quantity,
                visitDate, salesChannel, buyerPhone, customerId, dynamicPricingContext, null);
    }

    public TicketOrderRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String ticketTypeId,
            String priceRuleId,
            Integer quantity,
            LocalDate visitDate,
            String salesChannel,
            String buyerPhone,
            String customerId
    ) {
        this(
                idempotencyKey,
                operatorNote,
                evidenceAttachmentIds,
                ticketTypeId,
                priceRuleId,
                quantity,
                visitDate,
                salesChannel,
                buyerPhone,
                customerId,
                Map.of()
        );
    }
}
