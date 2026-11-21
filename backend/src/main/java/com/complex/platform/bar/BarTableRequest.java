package com.complex.platform.bar;

import java.util.List;

public record BarTableRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String barId,
        String tableName,
        String tableType,
        Long minimumSpendCent,
        Integer capacity,
        String status,
        // 第十八批 BL 段第 380 条 380-R1（BSM:717「酒吧要知道桌台、卡座、吧台属于哪个空间和商户。」）：台位所属空间（必填；
        // 须所属商户为该酒吧且启用中，见 BarTableSpaceBinding）。
        String spaceId
) {
    /** 380-R1 之前的 9 项构造（spaceId 为空），既有手工构造点不动。 */
    public BarTableRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String barId,
            String tableName, String tableType, Long minimumSpendCent, Integer capacity, String status) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, barId, tableName, tableType, minimumSpendCent, capacity, status, null);
    }
}
