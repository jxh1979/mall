package com.complex.platform.compliance;

import java.time.Instant;

/**
 * AUD 复核 P1-2 + AUD-021：企微 webhook nonce 防重放持久化。verify 通过验签后调用。
 *
 * <p>AUD-021：nonce 与报文指纹绑定登记——此前 nonce 先于业务事务独立落库，首次业务处理失败/响应丢失后，
 * 同一合法报文重试先被 401 REPLAY 挡住，瞬时故障可能永久漏同步。现改为：
 * 同 nonce + 同指纹 = 合法重投递（放行，交业务层幂等键精确重放/安全接管）；
 * 同 nonce + 异指纹 = 真重放攻击（拒绝）。
 */
public interface WecomWebhookNonceStore {

    enum Registration {
        /** 首见 nonce，已登记。 */
        FRESH,
        /** nonce 已存在且报文指纹一致=同一报文合法重投递，放行给业务幂等层。 */
        SAME_PAYLOAD,
        /** nonce 已存在但报文指纹不一致=nonce 被挪用于不同报文，判重放拒绝。 */
        REPLAY
    }

    /**
     * 原子登记 nonce+报文指纹并按上述语义返回结果。
     *
     * @param requestFingerprint 报文体规范化摘要（与验签 body_digest 同源）。
     * @param ttlSeconds 保留窗口（秒），实现据此清理过期行（超窗 nonce 亦被 timestamp 窗口二次拦截）。
     */
    Registration registerFresh(String nonce, String requestFingerprint, Instant now, long ttlSeconds);
}
