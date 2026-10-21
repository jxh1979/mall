package com.complex.platform.customer;

// 文件版本：V0.01

public record CustomerTicketProductResponse(
        String ticketTypeId,
        String ticketName,
        Long weekdayPriceCent,
        Long weekendPriceCent,
        Long holidayPriceCent,
        String currency,
        String status,
        // AUD-076 单店票挂酒吧：该票所属酒吧商户 id 与实时名称；null=综合体通票（前端展示“综合体通用”）。
        String barId,
        // TK-06（BSM:843「不设需要人脸照的票种；购票和转赠均不要求录入人脸照」）：不再下发人脸照要求与留存天数。
        String barName
) {
    // AUD-076 7 参兼容构造（无 bar）→ barId/barName=null（既有测试/控制器 mock 零改动）。
    public CustomerTicketProductResponse(
            String ticketTypeId, String ticketName, Long weekdayPriceCent, Long weekendPriceCent,
            Long holidayPriceCent, String currency, String status) {
        this(ticketTypeId, ticketName, weekdayPriceCent, weekendPriceCent, holidayPriceCent, currency, status,
                null, null);
    }
}
