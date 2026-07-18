package com.complex.platform.customer;

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
