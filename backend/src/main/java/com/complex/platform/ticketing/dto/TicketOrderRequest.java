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
        //   D-024：需要人脸照的票种，任何渠道下单都必须带满张数（柜台由工作人员现场录入，客人端本人拍摄），缺 → FACE_PHOTO_REQUIRED 拒单；
        //   其余票种此表为空、零影响。
        List<String> facePhotos,
        // V368 人脸照同意留痕 id(customer_consents.id)：客人端由 CustomerService 同事务落账后生成；
        //   员工端 POST /ticketing/orders 可传入会员同意 id，服务闸回查台账（存在/人脸照类型/已接受/未撤回，且属于本单客人），
        //   对不上即拒 FACE_PHOTO_CONSENT_INVALID（M009 F-05）；库上 chk_tfp_consent_present + fk_tfp_consent(V397) 兜底。
        String faceConsentId,
        // D-024 / R-005 柜台散客：客人在柜台设备上看过告知并现场确认同意后，由工作人员随下单一并提交 true——
        //   成单时以本单为主体落一条散客同意（同事务），再把工作人员录入的照片暂存到本单；只对 admin / kiosk 渠道生效。
        //   需要人脸照的票种既无 faceConsentId 又非 true → FACE_PHOTO_CONSENT_REQUIRED 拒单。
        Boolean faceConsentAccepted,
        // 柜台同意的采集终端标识（如 counter / kiosk-3），只作留痕；缺省记 counter。
        String faceConsentSourceTerminal
) {
    // ponytail: D-024 前的 14 参调用点（客人端 CustomerService / 既有测试）→ 无柜台同意字段。
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
            String barId,
            List<String> facePhotos,
            String faceConsentId
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, ticketTypeId, priceRuleId, quantity,
                visitDate, salesChannel, buyerPhone, customerId, dynamicPricingContext, barId, facePhotos, faceConsentId, null, null);
    }

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
