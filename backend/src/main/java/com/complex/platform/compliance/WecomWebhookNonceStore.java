package com.complex.platform.compliance;

import java.time.Instant;

/**
 * AUD 复核 P1-2：企微 webhook nonce 防重放持久化。verify 通过验签后调用；
 * 首见 nonce 记录并返回 true，重复 nonce 返回 false（拒重放）。TTL≈签名时间窗，过期行可清理。
 */
public interface WecomWebhookNonceStore {

    /**
     * 原子登记 nonce：首次登记返回 true；已存在（重放）返回 false。
     *
     * @param ttlSeconds 保留窗口（秒），实现据此清理过期行（超窗 nonce 亦被 timestamp 窗口二次拦截）。
     */
    boolean registerFresh(String nonce, Instant now, long ttlSeconds);
}
