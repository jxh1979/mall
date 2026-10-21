package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * TK-05 通票安全例外申请（BSM:804「客人出现重大情绪不稳定、安全问题或其他必要问题时，由夜班值班经理及以上批准处理，
 * 须记录原因、批准人与当时情形」）。slotIndex＝出事客人在该票中的人份序号（单人票为 1）；responsibleEntityId＝退款责任主体，
 * 多次通票恢复次数时即承担新增实际履约费用的具体域主管公司（主体主数据中在用的法律主体）。
 */
public record TicketSafetyEventRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String ticketInstanceId,
        Integer slotIndex,
        String reason,
        String situation,
        String responsibleEntityId
) {
}
