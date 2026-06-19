package com.complex.platform.terminal.dto;

import java.util.List;

/**
 * P0#9 设备生命周期流转请求（送修 / 挂失 / 找回 / 报废 通用）。
 * {@code reason} 为流转原因（必填，进留痕事件）。
 */
public record DeviceLifecycleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String staffId,
        String reason
) {
}
