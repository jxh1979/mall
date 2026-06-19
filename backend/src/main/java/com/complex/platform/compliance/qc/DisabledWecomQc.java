package com.complex.platform.compliance.qc;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认实现：能力开关 {@code wecom.ai_qc.enabled} 关时使用。
 * 不调任何 AI/外部，统一返回 {@code SKIPPED}；业务沿用现有基础关键词质检结果（risk_flags），不阻塞。
 */
public final class DisabledWecomQc implements WecomQcPort {

    private static final String MSG = "WeCom AI quality-check capability is disabled "
            + "(wecom.ai_qc.enabled=false); keyword-only quality check applies, AI not invoked";

    @Override
    public WecomQcResult analyzeMessage(WecomQcRequest request) {
        return WecomQcResult.skipped(MSG);
    }

    @Override
    public List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests) {
        List<WecomQcResult> results = new ArrayList<>();
        int count = requests == null ? 0 : requests.size();
        for (int i = 0; i < count; i++) {
            results.add(WecomQcResult.skipped(MSG));
        }
        return results;
    }
}
