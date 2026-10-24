package com.complex.platform.customer;

// 文件版本：V0.02

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
        String barName,
        // ROOT-03-R27（BSM 第 756 行「多种资格价不叠加，选择最终价格最低的一项」）：带客人会话时今日按本人资格系统判定的价格与命中的资格名称
        // （会员价、新人价、学生价；动态公开价最低时为空）；匿名为空。
        Long myPriceCent,
        String myPriceBasis,
        // ROOT-03 clients-R9（BSM 第 858 行「购票前须以门票守则的形式向客人告知该票的规则，包括能否退款、有效期、年龄限制与可使用范围」；
        // 第 879 行「展示……赠饮权益」）：能否退、有效天数、可使用范围（单店票＝所属酒吧，通票＝综合体通票）、所含权益名称、年龄限制（ENUM 第 33 行）。
        boolean refundable,
        Integer validityDays,
        String usageScope,
        java.util.List<String> entitlements,
        String ageLimitText
) {
    // AUD-076 7 参兼容构造（无 bar）→ barId/barName=null（既有测试/控制器 mock 零改动）。
    public CustomerTicketProductResponse(
            String ticketTypeId, String ticketName, Long weekdayPriceCent, Long weekendPriceCent,
            Long holidayPriceCent, String currency, String status) {
        this(ticketTypeId, ticketName, weekdayPriceCent, weekendPriceCent, holidayPriceCent, currency, status,
                null, null);
    }

    public CustomerTicketProductResponse(
            String ticketTypeId, String ticketName, Long weekdayPriceCent, Long weekendPriceCent,
            Long holidayPriceCent, String currency, String status, String barId, String barName) {
        this(ticketTypeId, ticketName, weekdayPriceCent, weekendPriceCent, holidayPriceCent, currency, status,
                barId, barName, null, null, false, null, null, java.util.List.of(), null);
    }
}
