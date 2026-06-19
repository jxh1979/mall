package com.complex.platform.compliance.qc;

import java.util.List;
import java.util.Map;

/**
 * 企微 AI 语义质检结果（端口出参，厂商无关）。永不抛受检异常以阻塞业务——
 * 开关关 / 无厂商 / 适配器异常均以状态码表达，关键词质检结果（risk_flags）始终独立保留、不受影响。
 *
 * @param status        质检状态
 * @param sentiment     情绪倾向（占位实现为 null；真实适配器回填，如 positive/neutral/negative）
 * @param complaintRisk 投诉倾向评分 0-100（占位实现为 null；真实适配器回填）
 * @param aiRiskFlags   AI 语义风险标签（与关键词 risk_flags 独立；占位实现为空集）
 * @param message       说明（留痕用）
 */
public record WecomQcResult(
        Status status,
        String sentiment,
        Integer complaintRisk,
        List<String> aiRiskFlags,
        String message
) {

    public enum Status {
        /** 开关关：AI 质检能力预留未启用，仅走关键词质检（不调 AI，不阻塞）。 */
        SKIPPED,
        /** 开关开但无真实厂商接入：占位适配器未配置，留接缝（不调外部、不崩）。 */
        NOT_CONFIGURED,
        /** AI 质检成功（真实适配器接入后使用）。 */
        ANALYZED,
        /** AI 适配器异常：不阻塞落库与关键词结果（fail-safe）。 */
        FAILED
    }

    public static WecomQcResult skipped(String message) {
        return new WecomQcResult(Status.SKIPPED, null, null, List.of(), message);
    }

    public static WecomQcResult notConfigured(String message) {
        return new WecomQcResult(Status.NOT_CONFIGURED, null, null, List.of(), message);
    }

    public static WecomQcResult failed(String message) {
        return new WecomQcResult(Status.FAILED, null, null, List.of(), message);
    }

    /** 落库用持久化状态码（对齐 V096 wecom_messages.ai_qc_status 枚举）。 */
    public String persistenceStatus() {
        return switch (status) {
            case SKIPPED -> "skipped";
            case NOT_CONFIGURED -> "not_configured";
            case ANALYZED -> "analyzed";
            case FAILED -> "failed";
        };
    }

    /** 是否产生了可落库的结构化结果（占位/跳过=否，业务据此决定是否回填 ai_qc_result）。 */
    public boolean hasStructuredResult() {
        return status == Status.ANALYZED
                && (sentiment != null || complaintRisk != null || !aiRiskFlags.isEmpty());
    }

    /** 结构化结果转 JSON 可序列化 map（仅 ANALYZED 时有内容；供 ai_qc_result 落库）。 */
    public Map<String, Object> toResultMap() {
        return Map.of(
                "sentiment", sentiment == null ? "" : sentiment,
                "complaint_risk", complaintRisk == null ? -1 : complaintRisk,
                "ai_risk_flags", aiRiskFlags
        );
    }
}
