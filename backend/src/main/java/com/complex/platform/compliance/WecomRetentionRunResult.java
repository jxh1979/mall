package com.complex.platform.compliance;

/** R5 T8 留存到期处理结果：各到期动作的处理会话数。 */
public record WecomRetentionRunResult(
        int archived,
        int deleted,
        int anonymized
) {
}
