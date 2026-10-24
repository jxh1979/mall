package com.complex.platform.bar;

// 文件版本：V0.02

import java.time.Instant;
import java.util.List;

public record BarReservationRecord(
        String id,
        String barId,
        String merchantId,
        String tableId,
        String customerId,
        Instant reservedAt,
        int guestCount,
        int allowedPassFreeGuestCount,
        long depositAmountCent,
        String depositStatus,
        String orderSource,
        String salesStaffId,
        String status,
        String createdTerminal,
        String deviceId,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt,
        String depositRule,
        String depositIntentId,
        Instant depositPaidAt,
        String depositSettlement,
        // V155 订卡邀请函：签发令牌（不可猜随机串）+ HMAC 防伪签名 + 卡座可进人数快照 + 状态（none/active/revoked）+ 签发时点。
        String invitationToken,
        String invitationSignature,
        Integer invitationCapacity,
        String invitationStatus,
        Instant invitationIssuedAt,
        // V264 数据存档与审批后删除范式（照 V262 酒吧目录先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // V519 TK-24：定金付款渠道（payDeposit 写入，达标退回按此「原路」）+ 定金处置时点（退回/退还/没收）。
        String depositChannel,
        Instant depositSettledAt,
        // V539 ROOT-03-R19：最晚到店时间（BSM 第 1062 行每笔必填）+ 台位排他预留状态（held/released，ENUM 第 100 行；只由占台/释放语句写）。
        Instant latestArrivalAt,
        String holdStatus
) {
    /** 向后兼容构造器（V539 前签名）：既有调用方零改动，最晚到店时间与预留状态默认空。 */
    public BarReservationRecord(
            String id,
            String barId,
            String merchantId,
            String tableId,
            String customerId,
            Instant reservedAt,
            int guestCount,
            int allowedPassFreeGuestCount,
            long depositAmountCent,
            String depositStatus,
            String orderSource,
            String salesStaffId,
            String status,
            String createdTerminal,
            String deviceId,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String depositRule,
            String depositIntentId,
            Instant depositPaidAt,
            String depositSettlement,
            String invitationToken,
            String invitationSignature,
            Integer invitationCapacity,
            String invitationStatus,
            Instant invitationIssuedAt,
            Instant archivedAt,
            String archivedBy,
            String deleteStatus,
            String deleteRequestedBy,
            Instant deleteRequestedAt,
            String deleteReason,
            String depositChannel,
            Instant depositSettledAt
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId, depositPaidAt, depositSettlement,
                invitationToken, invitationSignature, invitationCapacity, invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, null, null);
    }

    /** V539 ROOT-03-R19：落最晚到店时间（新建订台时）。 */
    public BarReservationRecord withLatestArrivalAt(Instant latestArrivalAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }

    /** 向后兼容构造器（V519 定金渠道/处置时点前签名）：既有调用方零改动，渠道与处置时点默认空。 */
    public BarReservationRecord(
            String id,
            String barId,
            String merchantId,
            String tableId,
            String customerId,
            Instant reservedAt,
            int guestCount,
            int allowedPassFreeGuestCount,
            long depositAmountCent,
            String depositStatus,
            String orderSource,
            String salesStaffId,
            String status,
            String createdTerminal,
            String deviceId,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String depositRule,
            String depositIntentId,
            Instant depositPaidAt,
            String depositSettlement,
            String invitationToken,
            String invitationSignature,
            Integer invitationCapacity,
            String invitationStatus,
            Instant invitationIssuedAt,
            Instant archivedAt,
            String archivedBy,
            String deleteStatus,
            String deleteRequestedBy,
            Instant deleteRequestedAt,
            String deleteReason
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId, depositPaidAt, depositSettlement,
                invitationToken, invitationSignature, invitationCapacity, invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                null, null);
    }

    /** V519 TK-24：只改定金流转字段，其余（邀请函、存档、删除）原样携带。 */
    public BarReservationRecord withDepositState(String depositStatus, String depositSettlement, String depositChannel,
            Instant depositPaidAt, Instant depositSettledAt, Instant updatedAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }

    /** ROOT-03-R4：客人在线付定金发起的当前付款意向（换渠道重新发起时替换，旧的作废）。 */
    public BarReservationRecord withDepositIntent(String intentId, Instant updatedAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, intentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }

    /** V155 邀请函签发/吊销：只改邀请函字段，其余（存档、定金渠道、最晚到店时间等）原样携带。 */
    public BarReservationRecord withInvitation(String invitationToken, String invitationSignature, Integer invitationCapacity,
            String invitationStatus, Instant invitationIssuedAt, Instant updatedAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }

    /** V519 TK-24：只改订台状态，其余原样携带（取消时保留定金渠道等资金留痕）。 */
    public BarReservationRecord withStatus(String status, Instant updatedAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }

    /** 向后兼容构造器（V264 存档/删除前签名）：既有调用方零改动，默认未存档 + delete_status=none。 */
    public BarReservationRecord(
            String id,
            String barId,
            String merchantId,
            String tableId,
            String customerId,
            Instant reservedAt,
            int guestCount,
            int allowedPassFreeGuestCount,
            long depositAmountCent,
            String depositStatus,
            String orderSource,
            String salesStaffId,
            String status,
            String createdTerminal,
            String deviceId,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String depositRule,
            String depositIntentId,
            Instant depositPaidAt,
            String depositSettlement,
            String invitationToken,
            String invitationSignature,
            Integer invitationCapacity,
            String invitationStatus,
            Instant invitationIssuedAt
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId, depositPaidAt, depositSettlement,
                invitationToken, invitationSignature, invitationCapacity, invitationStatus, invitationIssuedAt,
                null, null, "none", null, null, null);
    }

    /** 向后兼容构造器（V155 邀请函前签名）：既有调用方零改动，邀请函字段默认未签发（status=none）。 */
    public BarReservationRecord(
            String id,
            String barId,
            String merchantId,
            String tableId,
            String customerId,
            Instant reservedAt,
            int guestCount,
            int allowedPassFreeGuestCount,
            long depositAmountCent,
            String depositStatus,
            String orderSource,
            String salesStaffId,
            String status,
            String createdTerminal,
            String deviceId,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String depositRule,
            String depositIntentId,
            Instant depositPaidAt,
            String depositSettlement
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId, depositPaidAt, depositSettlement,
                null, null, null, "none", null);
    }

    /** 向后兼容构造器（T16 前签名）：既有调用方零改动，定金流转字段默认空。 */
    public BarReservationRecord(
            String id,
            String barId,
            String merchantId,
            String tableId,
            String customerId,
            Instant reservedAt,
            int guestCount,
            int allowedPassFreeGuestCount,
            long depositAmountCent,
            String depositStatus,
            String orderSource,
            String salesStaffId,
            String status,
            String createdTerminal,
            String deviceId,
            List<String> evidenceAttachmentIds,
            Instant createdAt,
            Instant updatedAt,
            String depositRule
    ) {
        this(id, barId, merchantId, tableId, customerId, reservedAt, guestCount, allowedPassFreeGuestCount,
                depositAmountCent, depositStatus, orderSource, salesStaffId, status, createdTerminal, deviceId,
                evidenceAttachmentIds, createdAt, updatedAt, depositRule, null, null, null);
    }

    // --- V264 存档/删除范式：不可变 record 的字段更新辅助。 ---
    public BarReservationRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }

    public BarReservationRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new BarReservationRecord(id, barId, merchantId, tableId, customerId, reservedAt, guestCount,
                allowedPassFreeGuestCount, depositAmountCent, depositStatus, orderSource, salesStaffId, status,
                createdTerminal, deviceId, evidenceAttachmentIds, createdAt, updatedAt, depositRule, depositIntentId,
                depositPaidAt, depositSettlement, invitationToken, invitationSignature, invitationCapacity,
                invitationStatus, invitationIssuedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                depositChannel, depositSettledAt, latestArrivalAt, holdStatus);
    }
}
