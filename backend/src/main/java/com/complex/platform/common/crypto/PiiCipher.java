package com.complex.platform.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import org.springframework.stereotype.Component;

/**
 * PII 字段加密（AES-256-GCM），落库密文、读时解密。密钥经 {@link KeyVault} 用途 {@code "pii"} 取。
 * 信封格式：{@code enc:v1:base64(iv(12) | ciphertext+tag)}；无该前缀的值=历史明文，原样透传（兼容存量行，
 * 无需一次性回填迁移）。密文被篡改时 GCM 认证失败抛错（fail-closed），不会静默返回错误明文。
 */
@Component
public class PiiCipher {

    private static final String PURPOSE = "pii";
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final KeyVault keyVault;
    private final SecureRandom secureRandom = new SecureRandom();

    public PiiCipher(KeyVault keyVault) {
        this.keyVault = keyVault;
    }

    /** 加密；null/空/已加密（带信封前缀）值原样返回——幂等，避免二次加密。 */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty() || plaintext.startsWith(PREFIX)) {
            return plaintext;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyVault.dataKey(PURPOSE), new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, envelope, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, envelope, IV_LENGTH, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PII encryption failed", exception);
        }
    }

    /** 解密；null/空/无信封前缀（历史明文）原样返回；密文被篡改→GCM 校验失败抛错。 */
    public String decrypt(String stored) {
        if (stored == null || stored.isEmpty() || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] envelope = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(envelope, 0, IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keyVault.dataKey(PURPOSE), new GCMParameterSpec(TAG_BITS, iv));
            byte[] plaintext = cipher.doFinal(Arrays.copyOfRange(envelope, IV_LENGTH, envelope.length));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PII decryption failed", exception);
        }
    }
}
