package com.complex.platform.common.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/** PII AES-256-GCM 加解密：往返、存量明文透传、幂等、篡改 fail-closed。无 Spring/DB。 */
class PiiCipherTests {

    // 固定 32 字节测试密钥（AES-256），不经 KeyVault 配置，纯验密码逻辑。
    private final PiiCipher cipher = new PiiCipher(
            purpose -> new SecretKeySpec("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES"));

    @Test
    void roundTripEncryptsThenDecrypts() {
        String plain = "13800000000";
        String encrypted = cipher.encrypt(plain);
        assertThat(encrypted).startsWith("enc:v1:").isNotEqualTo(plain);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    void decryptPassesThroughLegacyPlaintextAndNulls() {
        // 存量明文行无 enc: 前缀 → 原样返回，兼容不一次性回填。
        assertThat(cipher.decrypt("13800000000")).isEqualTo("13800000000");
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("")).isEmpty();
    }

    @Test
    void encryptIsIdempotentAndSkipsNullEmpty() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("")).isEmpty();
        String encrypted = cipher.encrypt("13800000000");
        // 已加密值带前缀，不二次加密。
        assertThat(cipher.encrypt(encrypted)).isEqualTo(encrypted);
    }

    @Test
    void tamperedCiphertextFailsClosed() {
        String encrypted = cipher.encrypt("13800000000");
        char[] chars = encrypted.toCharArray();
        int i = encrypted.length() - 3; // 篡改 base64 中部一个字节，保持长度合法
        chars[i] = chars[i] == 'A' ? 'B' : 'A';
        assertThatThrownBy(() -> cipher.decrypt(new String(chars))).isInstanceOf(RuntimeException.class);
    }
}
