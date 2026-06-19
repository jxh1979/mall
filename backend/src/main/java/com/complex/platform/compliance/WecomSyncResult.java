package com.complex.platform.compliance;

/** R5 T8 企微同步结果：会话 id + 落库消息数 + 命中复核（敏感质检）的消息数。 */
public record WecomSyncResult(
        String conversationId,
        int messagesStored,
        int flaggedForReview
) {
}
