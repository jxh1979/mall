package com.complex.platform.gate;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * AUD-010 ③ 设备凭证验签 nonce 防重放（{@code terminal_device_nonces}）。验签通过后消费 nonce：首次插入=接受；
 * 重复=重放拒绝。TTL 靠 {@code created_at} 定期清理（本 PR 不建清理 job）。存储异常 fail-closed（拒绝）——
 * 防重放宁可拒也不放行未去重的请求；仅影响带有效签名的设备绑定型写（灰度期极少触发）。
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
}
