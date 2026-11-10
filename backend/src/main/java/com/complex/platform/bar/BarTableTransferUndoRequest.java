package com.complex.platform.bar;

// 文件版本：V0.01

/** 第十八批 AB 段第 190 条 190-R3：撤销最近一次转台请求（BSM:1079「每次……撤销转台均须留痕。记录内容包括操作类型、原因、操作人、时间……」，原因必填）。 */
public record BarTableTransferUndoRequest(
        String idempotencyKey,
        Long sessionVersion,
        String reason
) {
}
