package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/** P0#4 存酒转赠流水（saved_bottle_transfers）：受让信息 + 操作员 + 证据留痕。 */
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
        Instant createdAt
) {
}
