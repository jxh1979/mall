package com.complex.platform.inventory;

// 文件版本：V0.01

import java.util.Map;

/** FS-017 退出前清点结果（BSM L1072「退出前清点存酒并通知客人在原店取回」）：按瓶计数、按物品汇总剩余量、已通知数。 */
public record SavedBottleExitNoticeResult(
        String merchantId,
        int bottleCount,
        int notifiedCount,
        Map<String, Integer> quantityByItem
) {
}
