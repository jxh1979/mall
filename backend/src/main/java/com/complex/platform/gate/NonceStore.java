package com.complex.platform.gate;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AUD-010 ③ 设备凭证验签 nonce 防重放（{@code terminal_device_nonces}）。验签通过后消费 nonce：首次插入=接受；
 * 重复=重放拒绝。存储异常 fail-closed（拒绝）——防重放宁可拒也不放行未去重的请求；仅影响带有效签名的设备绑定型写。
 *
 * <p>清理：nonce 行仅在容忍窗（±300s）内有意义，之外的行本就被时间戳先挡下。{@link #purgeExpired()} 定期删旧行
 * 防表无界增长（删除不影响重放防护）。
 */
@Component
public class NonceStore {

    private final JdbcTemplate jdbc;

    public NonceStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 原子消费：首次插入返回 true；重复（已存在）或存储异常返回 false（重放/不可去重→拒绝）。 */
    public boolean tryConsume(String deviceId, String nonce) {
        try {
            int inserted = jdbc.update(
                    "INSERT INTO terminal_device_nonces (device_id, nonce) VALUES (?, ?) "
                            + "ON CONFLICT (device_id, nonce) DO NOTHING",
                    deviceId, nonce);
            return inserted == 1;
        } catch (DataAccessException e) {
            return false;
        }
    }

    /**
     * 定期清理过期 nonce（默认每 30min，删 created_at 早于 1 小时的行——远超 ±300s 容忍窗，删了不影响重放防护）。
     * ponytail：固定阈值，无需按窗动态；调频靠 {@code access.device_credential.nonce_cleanup_ms}。
     */
    @Scheduled(fixedDelayString = "${access.device_credential.nonce_cleanup_ms:1800000}")
    public void purgeExpired() {
        try {
            jdbc.update("DELETE FROM terminal_device_nonces WHERE created_at < now() - interval '1 hour'");
        } catch (DataAccessException ignored) {
            // best-effort 清理；失败不影响重放防护（旧行仍被时间戳窗挡下）。
        }
    }
}
