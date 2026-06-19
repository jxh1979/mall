package com.complex.platform.compliance.qc;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 质检接缝实现：能力开关 {@code wecom.ai_qc.enabled} 开但当前无真实厂商接入时使用。
 *
 * <p>这是为真实 AI/LLM 语义质检 SDK 预留的接缝。当前返回 {@code NOT_CONFIGURED}（不崩、不调外部），
 * 接入真实厂商（云厂商语义/情绪/合规 API，或本地 LLM）后，在此实现内调用对应 SDK，
 * 并返回真实 {@code ANALYZED} 及 sentiment / complaintRisk / aiRiskFlags；适配器异常返回 {@code FAILED}
 * （仍不阻塞业务，关键词质检结果照常保留）。
 */
public final class ReservedWecomQc implements WecomQcPort {

    private static final String MSG = "WeCom AI quality-check enabled but no real provider adapter is configured "
            + "(AI/LLM semantic QC SDK pending; seam reserved, may bind a local LLM)";

    @Override
    public WecomQcResult analyzeMessage(WecomQcRequest request) {
        return WecomQcResult.notConfigured(MSG);
    }

    @Override
    public List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests) {
        List<WecomQcResult> results = new ArrayList<>();
        int count = requests == null ? 0 : requests.size();
        for (int i = 0; i < count; i++) {
            results.add(WecomQcResult.notConfigured(MSG));
        }
        return results;
    }
}
