package com.complex.platform.bar;

// 文件版本：V0.01

/**
 * 第十八批 R 段第 142 条 142-R2：交班单签字、财务见证归档请求，只带幂等键（签字人、见证人取当前登录人，BSM:940）。
 */
public record BarShiftActionRequest(
        String idempotencyKey
) {
}
