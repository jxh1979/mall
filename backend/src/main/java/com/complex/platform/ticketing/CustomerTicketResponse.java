package com.complex.platform.ticketing;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 1.8 客人端"我的票"安全投影：只给客人可见字段；
 * 内部分账（allocated_revenue_cent）、商户/场地内部 id、完整核销码均不出网。
 * AUD-077：transferState 标记转赠中("transferring")供"我的票"显示冻结态;其余为 null。
 * TK-13（ENUM L148「“我的票”按对应人份或次份显示补偿标注及其实际有效期」；BSM L862）：compensationKind 标注补偿补发行
 * （full_reissue＝全园故障/场次提前结束完整补发，挂原票原人份；null＝原票行）；补偿行的 validTo 即该人份实际补偿有效期。
 */
public record CustomerTicketResponse(
        String id,
        String ticketName,
        String entitlementType,
        String status,
        int totalTimes,
        int usedTimes,
        int remainingTimes,
        int numPeople,
        Instant validFrom,
        Instant validTo,
        // AUD-066：客人端"我的票"按父票聚合（一张卡片=票名+N人+剩余次数）。
        String parentTicketId,
        int slotIndex,
        String ticketNo,
        // AUD-077：转赠中标记(null=正常;"transferring"=该 slot 有在途转赠,冻结中不可用)。
        String transferState,
        String compensationKind
) {
    // ponytail: AUD-066 前不带 parent/slot/ticketNo 的调用点(测试)默认 null/0（未聚合父票）。
    public CustomerTicketResponse(String id, String ticketName, String entitlementType, String status,
            int totalTimes, int usedTimes, int remainingTimes, int numPeople, Instant validFrom, Instant validTo) {
        this(id, ticketName, entitlementType, status, totalTimes, usedTimes, remainingTimes, numPeople,
                validFrom, validTo, null, 0, null, null, null);
    }

    static CustomerTicketResponse of(IssuedEntitlementRecord record, String ticketName, String ticketNo) {
        return ofWithTransfer(record, ticketName, ticketNo, null);
    }

    static CustomerTicketResponse ofWithTransfer(IssuedEntitlementRecord record, String ticketName, String ticketNo,
            String transferState) {
        return new CustomerTicketResponse(record.id(), ticketName, record.entitlementType(), record.status(),
                record.totalTimes(), record.usedTimes(), record.remainingTimes(), record.numPeople(),
                record.validFrom(), record.validTo(),
                record.parentTicketId(), record.slotIndex(), ticketNo, transferState,
                record.compensationItemId() == null ? null : "full_reissue");
    }
}
