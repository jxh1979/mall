package com.complex.platform.terminal.dto;

import java.util.List;

/**
 * P0#9 违规处置请求：把违规置 handled=true 并落处置类型。
 * {@code disposition} ∈ acknowledged / device_blocked / dismissed；
 * device_blocked 联动设备 management_status→blocked + 撤销凭证。
 */
public record MdmViolationHandleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String disposition,
        String handledNote
) {
}
