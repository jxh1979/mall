package com.complex.platform.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * PII 盲索引：对明文（如手机号）计算确定性 HMAC-SHA256 摘要（64 位十六进制），用于按明文精确查找而不落明文。
 * 密钥经 {@link KeyVault} 用途 {@code "pii-index"} 取（与 PII 加密用途 {@code "pii"} 分离、独立密钥——
 * 单把钥匙泄露不同时打穿"查找"与"解密"两个面）。同明文恒定同摘要 → 可作唯一约束与等值查找；
 * 不可逆 → 泄露摘要不还原手机号。
 */
@Component
public class BlindIndex {

    private static final String PURPOSE = "pii-index";

    private final KeyVault keyVault;

    public BlindIndex(KeyVault keyVault) {
        this.keyVault = keyVault;
    }

    /** 计算盲索引；null/空白返回 null（无索引值）。输入按 trim 归一，保证同号一致命中。 */
    public String hash(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyVault.dataKey(PURPOSE).getEncoded(), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PII blind index unavailable", exception);
        }
    }
}
