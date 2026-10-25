package com.complex.platform.customer;

// 文件版本：V0.02

/** 订台选项。ROOT-03-R19：reserved＝该台位正被有效预留占着（ENUM 第 100 行），照常列出但不可下单。 */
public record CustomerBarReservationOptionResponse(
        String barId,
        String tableId,
        String tableType,
        String tableName,
        int capacity,
        int allowedPassFreeGuestCount,
        long minConsumptionCent,
        String currency,
        String status,
        boolean reserved,
        // ROOT-03 clients-R9（BSM 第 860 行「订台页面须写明该卡座适用的最低消费金额与服务费比例」；第 1076 行服务费只对卡座类台位收取）：
        // 卡座/包厢取所属项目当日已批准的收费规则（未配置为空），散台与吧台位为 0；depositAmountCent＝按本酒吧定金规则现算的应付定金（0＝免定金）。
        Integer serviceFeeRateBps,
        long depositAmountCent,
        // PGM 第 736 行「可以按全局、综合体……开关」：台位所属酒吧的综合体，客人端订台带 X-Project-Id。
        String projectId
) {
    public CustomerBarReservationOptionResponse(String barId, String tableId, String tableType, String tableName, int capacity,
            int allowedPassFreeGuestCount, long minConsumptionCent, String currency, String status, boolean reserved) {
        this(barId, tableId, tableType, tableName, capacity, allowedPassFreeGuestCount, minConsumptionCent, currency, status, reserved,
                null, 0L, null);
    }

    public CustomerBarReservationOptionResponse withDepositAmountCent(long amountCent) {
        return new CustomerBarReservationOptionResponse(barId, tableId, tableType, tableName, capacity, allowedPassFreeGuestCount,
                minConsumptionCent, currency, status, reserved, serviceFeeRateBps, amountCent, projectId);
    }

    /** 向后兼容构造器（V539 前签名）。 */
    public CustomerBarReservationOptionResponse(String barId, String tableId, String tableType, String tableName, int capacity,
            int allowedPassFreeGuestCount, long minConsumptionCent, String currency, String status) {
        this(barId, tableId, tableType, tableName, capacity, allowedPassFreeGuestCount, minConsumptionCent, currency, status, false);
    }
}
