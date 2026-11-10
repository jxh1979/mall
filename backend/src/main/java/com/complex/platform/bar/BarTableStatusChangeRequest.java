package com.complex.platform.bar;

// 文件版本：V0.01

/** 第十八批 AA 段第 206 条 206-R1：台位停用与恢复请求（ENUM:100「台位停用与恢复由店长及以上操作，填原因留痕」，原因必填）。 */
public record BarTableStatusChangeRequest(
        String idempotencyKey,
        String reason
) {
}
