package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;
import java.util.Map;

/**
 * FS-017 门店退出存酒交平台。items 每项：saved_bottle_id、received_quantity、condition（intact|damaged|short）；
 * 该商户全部有效期内未结存酒必须全部出现在 items；damaged/short 走作废路径（高价值须照片+视频复核）。
 */
public record SavedBottleHandoverRequest(
        String idempotencyKey,
        String operatorNote,
        String merchantId,
        String pickupLocation,
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds,
        List<Map<String, Object>> items
) {
}
