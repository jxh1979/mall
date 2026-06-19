package com.complex.platform.compliance.qc;

import java.util.List;

/**
 * 企微 AI 语义质检端口（端口/适配器模式，用户口径：第三方/AI 一律"预留接口 + 能力开关默认关"）。
 *
 * <p>所有真实 AI/LLM 语义质检交互（情绪识别、投诉倾向、语义风险研判）经此端口抽象，
 * 业务侧（{@code WecomConversationService} 落消息/质检）只依赖本接口，不直接耦合任何 AI 厂商 SDK。
 *
 * <p>路由由 {@link WecomQcRouter} 按能力开关 {@code wecom.ai_qc.enabled} 决定：
 * <ul>
 *   <li>开关关（默认）→ {@link DisabledWecomQc}：返回 {@code SKIPPED}，不调 AI，
 *       业务沿用现有基础关键词质检结果（risk_flags），不阻塞。</li>
 *   <li>开关开 → {@link ReservedWecomQc}：当前无真实厂商=占位实现，返回 {@code NOT_CONFIGURED}
 *       （留接缝，接入真实 AI/LLM SDK 或本地 LLM 后替换此实现）。</li>
 * </ul>
 *
 * <p>无论哪种实现，AI 质检结果均与关键词质检结果独立并存、互不覆盖；AI 侧任何状态都不得阻塞落库。
 */
public interface WecomQcPort {

    /** 单条消息语义质检。 */
    WecomQcResult analyzeMessage(WecomQcRequest request);

    /** 整段会话语义质检（多条消息一次分析，返回每条对应结果，顺序与入参一致）。 */
    List<WecomQcResult> analyzeConversation(List<WecomQcRequest> requests);
}
