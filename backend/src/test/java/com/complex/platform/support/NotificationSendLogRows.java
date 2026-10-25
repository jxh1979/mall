package com.complex.platform.support;

// 文件版本：V0.01

import com.complex.platform.common.crypto.PiiCipher;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.jdbc.core.JdbcTemplate;

/** notification_send_log.recipient 自 V547 起落 enc:v1 密文（随机 IV，SQL 不能按明文比）：测试按明文收件人筛行时取出解密再比。 */
public final class NotificationSendLogRows {

    private NotificationSendLogRows() {
    }

    /** 某域（eventKey 为 null 即不限事件）收件人解密后满足条件的行 id，新到旧。 */
    public static List<String> ids(JdbcTemplate jdbc, PiiCipher cipher, String domain, String eventKey, Predicate<String> recipient) {
        return jdbc.query("SELECT id, recipient FROM notification_send_log WHERE domain = ? AND (?::text IS NULL OR event_key = ?)"
                        + " ORDER BY created_at DESC, id",
                (rs, n) -> recipient.test(cipher.decrypt(rs.getString("recipient"))) ? rs.getString("id") : null,
                domain, eventKey, eventKey).stream().filter(Objects::nonNull).toList();
    }

    /** 删掉这些行及其短信幂等记录（测试清理）。 */
    public static void delete(JdbcTemplate jdbc, List<String> ids) {
        for (String id : ids) {
            jdbc.update("DELETE FROM idempotency_records WHERE idempotency_key = (SELECT request_id FROM notification_send_log WHERE id = ?)", id);
            jdbc.update("DELETE FROM notification_send_log WHERE id = ?", id);
        }
    }
}
