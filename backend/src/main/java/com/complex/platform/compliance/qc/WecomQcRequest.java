package com.complex.platform.compliance.qc;

import java.util.List;

/**
 * 企微 AI 语义质检入参（端口入参，厂商无关）。
 *
 * <p>仅传递质检所需的最小上下文：消息文本（脱敏后或原文引用，由调用方按合规口径决定）、
 * 既有关键词命中（供 AI 适配器参考叠加）、会话标识。占位实现不读取内容、不外传。
 *
 * @param conversationId 会话 id（留痕/关联用）
 * @param text           待质检文本
 * @param keywordFlags   既有关键词质检命中（供 AI 叠加参考，不被覆盖）
 */
public record WecomQcRequest(
        String conversationId,
        String text,
        List<String> keywordFlags
) {
}
