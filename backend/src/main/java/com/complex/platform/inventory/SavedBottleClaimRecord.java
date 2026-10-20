package com.complex.platform.inventory;

// 文件版本：V0.03

import java.time.Instant;
import java.util.List;

/**
 * 取酒流水。FS-016（BSM L1073「取酒核验、对应标签、取用前后剩余量、操作员工和时间必须留痕」）：
 * claimStaffId=当前操作员工、remainingBefore/After=取用前后剩余量、verificationMethod=sms|pickup_code|manual_exception、
 * verifiedPhoneHash=被核验的当前持有人手机哈希。
 */
public record SavedBottleClaimRecord(
        String id,
        String savedBottleId,
        int claimQuantity,
        String claimStaffId,
        String claimTerminalType,
        String claimDeviceId,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Integer remainingBefore,
        Integer remainingAfter,
        String verificationMethod,
        String verifiedPhoneHash,
        /** FS-017：门店退出交平台后取酒地点快照（BSM L867「并在指定地点领取」）。 */
        String pickupLocation
) {
    /** 向后兼容构造器（V0.02 签名）。 */
    public SavedBottleClaimRecord(
            String id, String savedBottleId, int claimQuantity, String claimStaffId, String claimTerminalType,
            String claimDeviceId, String status, List<String> evidenceAttachmentIds, Instant createdAt,
            Integer remainingBefore, Integer remainingAfter, String verificationMethod, String verifiedPhoneHash
    ) {
        this(id, savedBottleId, claimQuantity, claimStaffId, claimTerminalType, claimDeviceId, status,
                evidenceAttachmentIds, createdAt, remainingBefore, remainingAfter, verificationMethod, verifiedPhoneHash, null);
    }

    /** 向后兼容构造器（V0.01 签名）：既有调用方零改动。 */
    public SavedBottleClaimRecord(
            String id,
            String savedBottleId,
            int claimQuantity,
            String claimStaffId,
            String claimTerminalType,
            String claimDeviceId,
            String status,
            List<String> evidenceAttachmentIds,
            Instant createdAt
    ) {
        this(id, savedBottleId, claimQuantity, claimStaffId, claimTerminalType, claimDeviceId, status,
                evidenceAttachmentIds, createdAt, null, null, null, null, null);
    }
}
