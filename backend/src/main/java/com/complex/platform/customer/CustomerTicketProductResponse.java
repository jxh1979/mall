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
        String barName,
        // V359：该票种是否要求购票人拍入场人脸照（票种所绑权益模板任一 require_face_photo=TRUE 即为 TRUE）。
        //   客人端据此才弹拍照；不需要的票种一律不问客人要生物识别信息（PIPL 最小必要）。
        boolean requireFacePhoto,
        // V359：人脸照留存天数（后台可配，当前生效值）。客人端拍照前必须如实告知照片留多久，写死会变成假告知。
        //   requireFacePhoto=false 时为 null（不告知无关的东西）。
        Integer facePhotoRetentionDays
) {
    // ponytail: V359 前带 bar 但不带脸照两列的调用点 → 不要脸照。
    public CustomerTicketProductResponse(
            String ticketTypeId, String ticketName, Long weekdayPriceCent, Long weekendPriceCent,
            Long holidayPriceCent, String currency, String status, String barId, String barName) {
        this(ticketTypeId, ticketName, weekdayPriceCent, weekendPriceCent, holidayPriceCent, currency, status,
                barId, barName, false, null);
    }

    // AUD-076 7 参兼容构造（无 bar）→ barId/barName=null（既有测试/控制器 mock 零改动）。
    public CustomerTicketProductResponse(
            String ticketTypeId, String ticketName, Long weekdayPriceCent, Long weekendPriceCent,
            Long holidayPriceCent, String currency, String status) {
        this(ticketTypeId, ticketName, weekdayPriceCent, weekendPriceCent, holidayPriceCent, currency, status,
                null, null);
    }
}
