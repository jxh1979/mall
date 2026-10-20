package com.complex.platform.inventory;

// 文件版本：V0.02

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * P0#4 存酒转赠流水（saved_bottle_transfers）：受让信息 + 操作员 + 证据留痕。
 * FS-016（BSM L1198「完整记录转让前后客户、时间与操作来源」）：operationSource=终端来源、fromPhoneHash=转让前持有人手机哈希、
 * toCustomerId=受让人客户 id（按手机号查到则落，散客为空）。
 */
public record SavedBottleTransferRecord(
        String id,
        String savedBottleId,
        String fromCustomerId,
        String fromNameOrNickname,
        String toNameOrNickname,
        @JsonIgnore String toPhone,
        @JsonIgnore String toPhoneHash,
        int transferQuantity,
        String operatorId,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        String operationSource,
        @JsonIgnore String fromPhoneHash,
        String toCustomerId
) {
    /** 向后兼容构造器（V0.01 签名）：既有调用方零改动。 */
    public SavedBottleTransferRecord(
            String id,
            String savedBottleId,
            String fromCustomerId,
            String fromNameOrNickname,
            String toNameOrNickname,
            String toPhone,
            String toPhoneHash,
            int transferQuantity,
            String operatorId,
            String status,
            List<String> evidenceAttachmentIds,
            Instant createdAt
    ) {
        this(id, savedBottleId, fromCustomerId, fromNameOrNickname, toNameOrNickname, toPhone, toPhoneHash,
                transferQuantity, operatorId, status, evidenceAttachmentIds, createdAt, null, null, null);
    }
}
