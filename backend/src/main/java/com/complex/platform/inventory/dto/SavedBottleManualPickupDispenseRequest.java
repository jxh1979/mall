package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;
import java.util.Map;

/** FS-016 人工例外取酒第四步：本人到场放酒（present_in_person 须为 true，BSM L1073「只允许存酒客户本人到场办理」）。 */
public record SavedBottleManualPickupDispenseRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        List<Map<String, Object>> claimItems,
        Boolean presentInPerson
) {
}
