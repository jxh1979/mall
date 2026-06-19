package com.complex.platform.analytics;

import com.complex.platform.common.PiiTextRedactor;

/**
 * AI 质检输入脱敏入口；规则已上移到 {@link PiiTextRedactor}（R5 T8 企微合规链共用），此处仅薄委派，
 * 保持 analytics 既有调用面与返回类型不变。
 */
final class AiTextRedactor {

    private AiTextRedactor() {
    }

    static RedactedText redactAndTruncate(String value, int maxChars) {
        PiiTextRedactor.Result result = PiiTextRedactor.redactAndTruncate(value, maxChars);
        return new RedactedText(result.value(), result.redactionApplied());
    }

    record RedactedText(String value, boolean redactionApplied) {
    }
}
